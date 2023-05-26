package org.octopusden.octopus.mcsdistributionbuilder.tasks;

import org.octopusden.octopus.mcsdistributionbuilder.fileutils.FileUtils;

import java.util.ArrayList;
import java.util.concurrent.RecursiveTask;

@SuppressWarnings({ "NullableProblems" })
public class FindAllFilesFromClientConfigurationTask extends RecursiveTask<ArrayList<String>> {
    String ProgramConfigFileName;
    ArrayList<String> ConfigFilesXPathList;
    ArrayList<String> ConfigFilesList;
    ArrayList<String> FilesXPathList;
    String FileExtension;

    public FindAllFilesFromClientConfigurationTask( String programConfigFileName, ArrayList<String> configFilesXPathList, ArrayList<String> configFilesList, ArrayList<String> filesXPathList, String fileExtension )
    {
        ProgramConfigFileName = programConfigFileName;
        ConfigFilesXPathList = configFilesXPathList;
        ConfigFilesList = configFilesList;
        FilesXPathList = filesXPathList;
        FileExtension = fileExtension;
    }

    @Override
    protected ArrayList<String> compute()
    {
        ArrayList<String> FilesList = new ArrayList<>();

        // Use ProgramConfigFileName only for grabbing directory
        // Get Main Config File Dir
        StringBuffer DirName = new StringBuffer();
        FileUtils.DevideFilePathIntoParts( ProgramConfigFileName, DirName, null );

        ArrayList<RecursiveFindFilesInClientConfigurationTask> tasks = new ArrayList<>( ConfigFilesList.size() );

        // Process all config files
        for ( String configFile : ConfigFilesList )
        {
            RecursiveFindFilesInClientConfigurationTask task = new RecursiveFindFilesInClientConfigurationTask( DirName.toString(),
                    configFile,
                    ConfigFilesXPathList,
                    FilesXPathList,
                    FileExtension );
            task.fork();
            tasks.add( task );
        }

        // Now Combine all recursive tasks result in one
        for ( RecursiveFindFilesInClientConfigurationTask task : tasks )
        {
            ArrayList<String> list = task.join();
            // check for failure
            if ( list == null ) return ( null );
            // Add new Files
            FilesList.addAll( list );
        }

        return ( FilesList );
    }

}
