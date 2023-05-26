package org.octopusden.octopus.mcsdistributionbuilder.fileutils;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@SuppressWarnings({ "JavaDoc" })
public class FileUtils {

    /**
     * Procedure separates filepath into Path and FileName.
     *
     * @param FilePath - Input File Path
     * @return true if succesful, false otherwise
     */
    public static boolean DevideFilePathIntoParts( String FilePath,
                                                   StringBuffer DirName,
                                                   StringBuffer FileName )
    {
        int iSepPos = FilePath.lastIndexOf( File.separator );

        if ( iSepPos > 0 )
        {
            if ( DirName != null )
            {
                DirName.append( FilePath.substring( 0, iSepPos + 1 ) );
            }
            if ( FileName != null )
            {
                FileName.append( FilePath.substring( iSepPos + 1 ) );
            }
            return true;
        } else
        {
            return false;
        }
    }


    /**
     * Reads String  from InputStream and saves it to String
     */
    public static String InputStreamToString( InputStream is )
    {
        String res = "";

        int ch;
        try
        {
            while ( ( ( ch = is.read() ) != -1 ) && ( ch != 0 ) )
            {
                res += (char) ch;
            }
        } catch ( IOException ex )
        {
            System.err.println( "I/O Exception [" + ex.getMessage() + "]" );
        }

        return res;
    }


}
