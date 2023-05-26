# MCSDistributionBuilder
This tool is used in MultiChannelServer distribution packing jobs.

**MultiChannelServer** is an abstract product. There are many propieritary implementations of such servers with different names but similar configuration structure.

This builder takes such a configuration as a first argument and path to folder with *RPM* or *DEB* packages as second one. The result is a separate forder with those *RPM* or *DEB* packages needed for configuration supplied to work.

**WARNINGS**: 
- Previous versions of this tool was for filesystem-like package search in *FILE* tag. Starting from version 01.03 it takes real *Java* regular expressions for this.
- Former macro `$RPMDIR` is replaced to `$PKGDIR` since *DEB* packages are supported also.
- *DEB* packages support is partial (not full): no search for libraries inside them is implemented yet. Versins before 01.13 has no *DEB* support at all.
- *Windows-style* path separators support in all configuration files is **deprecated**. Please use Unix-style separator only.
