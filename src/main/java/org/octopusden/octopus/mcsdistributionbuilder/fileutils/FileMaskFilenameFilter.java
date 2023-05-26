package org.octopusden.octopus.mcsdistributionbuilder.fileutils;

import java.io.File;
import java.io.FilenameFilter;


@SuppressWarnings({ "JavaDoc" })
public class FileMaskFilenameFilter
        implements FilenameFilter {
    private String FileNameRegExp;


    public FileMaskFilenameFilter( String AFileNameRegexp )
    {
        SetFileNameMask( AFileNameRegexp );
    }


    public void SetFileNameMask( String AFileNameRegexp )
    {
        this.FileNameRegExp = AFileNameRegexp;
        System.out.println("Regexp: " + FileNameRegExp);
    }


    public boolean accept( File dir, String name )
    {
        return ( name.matches( FileNameRegExp ) );
    }
}
