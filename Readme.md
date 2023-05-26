MCSDistributionBuilder tool, used in MultiChannelServer distribution packing jobs.

MultiChannelServer is an abstract product. There are many propieritary implementations of such servers with different names but similar configuration structure.
This builder takes such a configuration as an argument and foler with RPM/DEB packages. The result is a separate forder with those RPM/DEB packages needed for configuration supplied to work.

**WARNINGS**: 
- Previous versions of this tool was for filesystem-like package search in *FILE* tag. Starting from version 01.03 it takes real *java* regular expressions for this.
- Former macro `$PKGDIR` is replaced to `$PKGDIR` since *DEB* packages are supported also.
- *DEB* packages support is partial (not full), no search for libraries inside them is implemented yet.
- *Windows-style* path separators support in all configuration files is **deprecated**. Please use Unix-style separator only.
