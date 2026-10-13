/**
 * The entry a path belongs to: the longest url it equals or sits under, so
 * "/settings/security" still lights "Settings".
 */
export function activeUrl(pathname: string, urls: string[]): string | undefined {
  const path = pathname.replace(/\/+$/, "") || "/";
  return urls
    .filter((url) => path === url || (url !== "/" && path.startsWith(url + "/")))
    .sort((left, right) => right.length - left.length)[0];
}
