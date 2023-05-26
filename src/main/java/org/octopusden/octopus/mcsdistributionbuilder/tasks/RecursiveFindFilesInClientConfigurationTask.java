package org.octopusden.octopus.mcsdistributionbuilder.tasks;

import org.octopusden.octopus.mcsdistributionbuilder.fileutils.FileMaskFilenameFilter;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

@SuppressWarnings({ "NullableProblems" })
public class RecursiveFindFilesInClientConfigurationTask extends RecursiveTask<ArrayList<String>> {
    String RootDirName;
    String ConfigFileName;
    ArrayList<String> ConfigFilesXPathList;
    ArrayList<String> FilesXPathList;
    String FileExtension;

    public RecursiveFindFilesInClientConfigurationTask( String rootDirName, String configFileName, ArrayList<String> configFilesXPathList, ArrayList<String> filesXPathList, String fileExtension )
    {
        RootDirName = rootDirName;
        ConfigFileName = configFileName;
        ConfigFilesXPathList = configFilesXPathList;
        FilesXPathList = filesXPathList;
        FileExtension = fileExtension;
    }

    @Override
    protected ArrayList<String> compute()
    {
        ArrayList<String> FilesList = new ArrayList<>();

        String FullConfigFileName = RootDirName + File.separator + ConfigFileName;
        Document document;
        SAXReader oXmlReader;

        // Parse MultiChannelServer configuration file ()
        try
        {
            oXmlReader = new SAXReader();
            document = oXmlReader.read( new FileReader( FullConfigFileName ) );
        } catch ( FileNotFoundException e )
        {
            // Remove unnessary logging
            //System.err.println( "  ERROR: File [" + ConfigFileName + "] NOT FOUND." );
            // Ignore this error
            return FilesList;
        } catch ( DocumentException e )
        {
            System.err.println( "  ERROR: File  [" + ConfigFileName + "] non-XML or CORRUPTED [" +
                    e.getMessage() + "]" );
            return null;
        }

        // Step 1: Looking for our files
        for ( String aFilesXPathList : FilesXPathList )
        {
            // Find referenced config files
            List filesList = document.selectNodes( aFilesXPathList );

            for ( Object aFilesList : filesList )
            {
                String sFile;
                if ( aFilesList instanceof String )
                {
                    sFile = (String) aFilesList;
                } else if ( aFilesList instanceof Attribute )
                {
                    sFile = ( (Attribute) aFilesList ).getValue();
                } else
                {
                    System.err.println( "  ERROR: Invalid XPath result for expression [" + aFilesXPathList + "]." );
                    return null;
                }

                if ( sFile.startsWith( File.separator ) )
                {
                    sFile = sFile.substring( 1 );
                }

                // Create Filter Mask
                FileMaskFilenameFilter FNFilter = new FileMaskFilenameFilter( FileExtension );

                // Add to our needable file list
                if ( FNFilter.accept( null, sFile ) )
                {
                    FilesList.add( sFile );
                }
            }
        }

        ArrayList<RecursiveFindFilesInClientConfigurationTask> tasks = new ArrayList<>();

        for ( int i = 0; i < ConfigFilesXPathList.size(); i++ )
        {
            // Find referenced config files
            List filesList = document.selectNodes( ConfigFilesXPathList.get( i ) );

            for ( Object aFile : filesList )
            {
                String sFile;
                if ( aFile instanceof String )
                {
                    sFile = (String) aFile;
                } else if ( aFile instanceof Attribute )
                {
                    sFile = ( (Attribute) aFile ).getValue();
                } else
                {
                    System.err.println( "  ERROR: Invalid XPath result for expression ["
                            + ConfigFilesXPathList.get( i ) + "]." );
                    return null;
                }

                if ( sFile.startsWith( File.separator ) )
                {
                    sFile = sFile.substring( 1 );
                }

                File file = new File( RootDirName + sFile );
                if ( !file.exists() )
                {
                    // Remove unnessary logging
                    // System.err.println( "  ERROR: File [" + RootDirName + sFile + "] NOT FOUND." );
                } else
                {

                    // Add only XML config Files,we are interested only in those.

                    // Recursive processing Config File
                    if ( sFile.toLowerCase().endsWith( ".xml" ) )
                    {
                        RecursiveFindFilesInClientConfigurationTask task = new RecursiveFindFilesInClientConfigurationTask( RootDirName,
                                sFile,
                                ConfigFilesXPathList,
                                FilesXPathList,
                                FileExtension );
                        task.fork();
                        tasks.add( task );
                    }
                }
            }
        }

        // Now Combine all recursive tasks result in one
        for ( RecursiveFindFilesInClientConfigurationTask task : tasks )
        {
            ArrayList<String> list = task.join();
            // check for failure
            if ( list == null ) return ( null );
            FilesList.addAll( list );
        }

        return FilesList;
    }
}
