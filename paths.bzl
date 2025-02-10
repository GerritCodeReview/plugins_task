# Borrowed from https://github.com/bazelbuild/bazel-skylib/blob/main/lib/paths.bzl

def _is_absolute(path):
    """Returns `True` if `path` is an absolute path.

    Args:
      path: A path (which is a string).

    Returns:
      `True` if `path` is an absolute path.
    """
    return path.startswith("/") or (len(path) > 2 and path[1] == ":")

def join(path, *others):
    """Joins one or more path components intelligently.

    This function mimics the behavior of Python's `os.path.join` function on POSIX
    platform. It returns the concatenation of `path` and any members of `others`,
    inserting directory separators before each component except the first. The
    separator is not inserted if the path up until that point is either empty or
    already ends in a separator.

    If any component is an absolute path, all previous components are discarded.

    Args:
      path: A path segment.
      *others: Additional path segments.

    Returns:
      A string containing the joined paths.
    """
    result = path

    for p in others:
        if _is_absolute(p):
            result = p
        elif not result or result.endswith("/"):
            result += p
        else:
            result += "/" + p

    return result
