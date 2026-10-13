"""Fails when a SARIF report has a finding of High or Critical severity.

Code scanning only blocks merges through rules a repository admin sets, so
CI applies the same threshold itself: a security severity of 7.0 or more
(GitHub's "High" and "Critical") fails the job, everything below only shows
up in the Security tab.

Usage: python3 .github/security/sarif_gate.py <report.sarif or directory>
"""

import json
import pathlib
import sys

THRESHOLD = 7.0


def severity(rule):
    properties = (rule or {}).get("properties", {})
    try:
        return float(properties.get("security-severity", 0))
    except (TypeError, ValueError):
        return 0.0


def findings(report):
    for run in json.loads(report.read_text(encoding="utf-8")).get("runs", []):
        rules = {}
        for tool in [run["tool"]["driver"], *run["tool"].get("extensions", [])]:
            for rule in tool.get("rules", []):
                rules[rule["id"]] = rule
        for result in run.get("results", []):
            if result.get("suppressions"):
                continue
            rule = rules.get(result.get("ruleId"))
            if severity(rule) >= THRESHOLD:
                location = result["locations"][0]["physicalLocation"]
                yield (severity(rule), result["ruleId"],
                       location["artifactLocation"]["uri"],
                       location.get("region", {}).get("startLine", "?"))


def main(target):
    path = pathlib.Path(target)
    reports = sorted(path.rglob("*.sarif")) if path.is_dir() else [path]
    blocking = [finding for report in reports for finding in findings(report)]
    for level, rule, uri, line in sorted(blocking, reverse=True):
        print(f"::error file={uri},line={line}::{rule} "
              f"(security severity {level})")
    print(f"{len(blocking)} finding(s) of High or Critical severity "
          f"in {len(reports)} report(s)")
    return 1 if blocking else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
