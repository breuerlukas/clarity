package de.lukasbreuer.clarity.api.web.space;

import de.lukasbreuer.clarity.api.space.Space;
import de.lukasbreuer.clarity.api.space.SpaceKind;
import de.lukasbreuer.clarity.api.space.SpaceRepository;
import de.lukasbreuer.clarity.api.space.SpaceSlug;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Creates, renames and removes spaces. The kind is chosen once; a space only
 * goes once its banks and mailboxes are removed and its contracts and
 * invoices deleted, and the last one stays. Error codes: 5000 invalid name,
 * icon or kind, 5001 unknown space, 5002 space still has banks, mailboxes,
 * contracts or invoices, 5003 last space.
 */
@RestController
@RequiredArgsConstructor
public final class SpaceController {
  private static final int MAXIMUM_NAME_LENGTH = 40;
  private static final String ICON_PATTERN = "[a-z0-9-]{1,32}";

  private final SpaceRepository spaceRepository;
  private final SpaceContents spaceContents;

  @RequestMapping(path = "/spaces/", method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> findSpaces() {
    return spaceRepository.findAll()
      .thenApply(spaces -> ApiResponse.success(Map.of("spaces", spaces.stream()
        .sorted(Comparator.comparing(Space::createdAt))
        .map(Space::information)
        .toList())));
  }

  @RequestMapping(path = "/spaces/create/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> create(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var name = body.getString("name", MAXIMUM_NAME_LENGTH).trim();
    var icon = body.getString("icon", 32);
    var kind = SpaceKind.find(body.getString("kind", 16));
    if (name.isEmpty() || !icon.matches(ICON_PATTERN) || kind.isEmpty()) {
      return ApiResponse.error(5000).future();
    }
    return spaceRepository.findAll()
      .thenCompose(spaces -> spaceRepository.save(Space.create(
        UUID.randomUUID(), SpaceSlug.of(slugs(spaces, null)).forName(name),
        name, icon, kind.get(), System.currentTimeMillis())))
      .thenApply(space -> ApiResponse.success(Map.of("space",
        space.information())));
  }

  /**
   * A new name brings a new slug, so addresses keep reading like the name.
   */
  @RequestMapping(path = "/spaces/update/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> update(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var spaceId = body.getUUID("id");
    var name = body.getString("name", MAXIMUM_NAME_LENGTH).trim();
    var icon = body.getString("icon", 32);
    if (spaceId == null) {
      return ApiResponse.error(5001).future();
    }
    if (name.isEmpty() || !icon.matches(ICON_PATTERN)) {
      return ApiResponse.error(5000).future();
    }
    return spaceRepository.findAll()
      .thenCompose(spaces -> update(spaces, spaceId, name, icon));
  }

  private CompletableFuture<ApiResponse> update(
    List<Space> spaces, UUID spaceId, String name, String icon
  ) {
    var space = spaces.stream().filter(entry -> entry.id().equals(spaceId))
      .findFirst();
    if (space.isEmpty()) {
      return ApiResponse.error(5001).future();
    }
    var slug = name.equals(space.get().name()) ? space.get().slug() :
      SpaceSlug.of(slugs(spaces, spaceId)).forName(name);
    space.get().change(name, slug, icon);
    return spaceRepository.save(space.get())
      .thenApply(saved -> ApiResponse.success(Map.of("space",
        saved.information())));
  }

  @RequestMapping(path = "/spaces/delete/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> delete(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var spaceId = ApiRequestBody.of(payload, response).getUUID("id");
    if (spaceId == null) {
      return ApiResponse.error(5001).future();
    }
    return spaceRepository.findAll()
      .thenCompose(spaces -> delete(spaces, spaceId));
  }

  private CompletableFuture<ApiResponse> delete(
    List<Space> spaces, UUID spaceId
  ) {
    var space = spaces.stream().filter(entry -> entry.id().equals(spaceId))
      .findFirst();
    if (space.isEmpty()) {
      return ApiResponse.error(5001).future();
    }
    if (spaces.size() == 1) {
      return ApiResponse.error(5003).future();
    }
    return spaceContents.isEmpty(space.get())
      .thenCompose(empty -> empty ?
        spaceRepository.delete(space.get())
          .thenApply(_ -> ApiResponse.success()) :
        ApiResponse.error(5002).future());
  }

  /**
   * @param except The space being renamed, whose own slug is free for it
   */
  private Set<String> slugs(List<Space> spaces, UUID except) {
    return spaces.stream()
      .filter(space -> !space.id().equals(except))
      .map(Space::slug)
      .collect(Collectors.toSet());
  }
}