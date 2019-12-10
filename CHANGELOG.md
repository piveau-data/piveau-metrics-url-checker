# ChangeLog

## [1.0.0](https://gitlab.fokus.fraunhofer.de/viaduct/metrics/metrics-url-checker/-/tags/1.0.0) (2019-12-05)

**Changed:**
* Bumped Vert.X version to 4.0.0-milestone3
* Changed required Java version to 11
* URLs are checked with a HTTP HEAD instead of GET request

**Removed:**
* Maven Wrapper
* Redeploy scripts

**Fixed:**
* All errors related to the Vert.X version bump
