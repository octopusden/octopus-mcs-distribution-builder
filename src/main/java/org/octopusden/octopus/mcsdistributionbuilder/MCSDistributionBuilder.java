package org.octopusden.octopus.mcsdistributionbuilder;


import org.octopusden.octopus.mcsdistributionbuilder.fileutils.FileMaskFilenameFilter;
import org.octopusden.octopus.mcsdistributionbuilder.fileutils.FileUtils;
import org.octopusden.octopus.mcsdistributionbuilder.logutils.Logger;
import org.octopusden.octopus.mcsdistributionbuilder.tasks.FindAllFilesFromClientConfigurationTask;
import org.octopusden.octopus.mcsdistributionbuilder.stringutils.stringutils;
import org.octopusden.octopus.mcsdistributionbuilder.variables.IVariablesStorage;
import org.octopusden.octopus.mcsdistributionbuilder.variables.VariablesStorage;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.*;
import java.text.Collator;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

import static java.nio.file.StandardCopyOption.*;

@SuppressWarnings({ "JavaDoc", "NullableProblems" })
public class MCSDistributionBuilder {
    private static final String XPath_Includes = "/PROFILE/INCLUDE[@FILE]";
    private static final String const_File = "FILE";
    private static final String const_Name = "NAME";
    private static final String const_XPATH = "XPATH";
    private static final String const_XPATH_COND = "XPATH_COND";

    private static final String const_PKGDIR_Variable = "PKGDIR";
    private static final String XPath_Config_Files = "/PROFILE/CONFIGS/CONFIG[@FILE]";
    private static final String XPath_Module_Files = "/PROFILE/MODULES/MODULE[@FILE]";

    private static final String XPath_Config_XPaths = "/PROFILE/CONFIGREFS/REF[@XPATH]";
    private static final String XPath_Module_XPaths = "/PROFILE/MODULEREFS/REF[@XPATH]";

    private static final String XPath_Inst_Files = "/PROFILE/FILE[@NAME]";
    private static final String XPath_Variables = "/PROFILE/VARIABLE[@NAME]";

    static final ForkJoinPool mainPool = new ForkJoinPool();

    private static void PrintHelp()
    {
        System.out.println( "\nMultiChannelServer Distribution builder ($Revision$)" );
        System.out.println( "Usage: MCSDistributionBuilder <ConfigFileName> <Output Dir>\n " );
    }


    private static boolean LoadConfigFile( String ConfigFileName,
                                           IVariablesStorage VarStorage,
                                           ArrayList<CInstFile> InstFiles,
                                           ArrayList<String> ConfigFilesXPathList,
                                           ArrayList<String> ConfigFilesList,
                                           ArrayList<String> ModuleFilesXPathList,
                                           ArrayList<String> ModuleFilesList ) throws ParserConfigurationException
    {
        boolean res;
        Document document;
        SAXReader oXmlReader;

        // Parse main configuration file ()
        try
        {
            oXmlReader = new SAXReader();
            document = oXmlReader.read( new FileReader( ConfigFileName ) );
        } catch ( FileNotFoundException e )
        {
            System.err.println( "  ERROR: File [" + ConfigFileName + "] NOT FOUND." );
            return false;
        } catch ( DocumentException e )
        {
            System.err.println( "  ERROR: File  [" + ConfigFileName + "] non-XML or CORRUPTED [" +
                    e.getMessage() + "]" );
            return false;
        }

        // Work on Include Files
        List IncludesList = document.selectNodes( XPath_Includes );
        res = true; // because cycle
        for ( Iterator i = IncludesList.iterator(); i.hasNext() && res; )
        {
            Element elm = (Element) i.next();
            // Get Main Config File Dir
            StringBuffer DirName = new StringBuffer();
            FileUtils.DevideFilePathIntoParts( ConfigFileName, DirName, null );
            // Add it to our included config name
            String FileName = DirName + File.separator + elm.attribute( const_File ).getStringValue();
            res = LoadConfigFile( FileName, VarStorage, InstFiles, ConfigFilesXPathList, ConfigFilesList,
                    ModuleFilesXPathList,
                    ModuleFilesList );
        }

        // Load Variables
        List VariablesList = document.selectNodes( XPath_Variables );
        for ( Object aVariablesList : VariablesList )
        {
            Element elm = (Element) aVariablesList;

            String varName = elm.attribute( const_Name ).getStringValue();
            String varValue = elm.getStringValue();
            VarStorage.SetVariableValue( varName, varValue );
            System.out.println( "  Found Variable '" + varName + "'='" +
                    VarStorage.GetVariableValue( varName ) + "'" );
        }

        // Load Configs Files
        List FilesList = document.selectNodes( XPath_Config_Files );
        for ( Object aFilesList : FilesList )
        {
            Element elm = (Element) aFilesList;
            ConfigFilesList.add( elm.attribute( const_File ).getStringValue() );
        }

        // Load Configs XPaths
        FilesList = document.selectNodes( XPath_Config_XPaths );
        for ( Object aFilesList : FilesList )
        {
            Element elm = (Element) aFilesList;
            ConfigFilesXPathList.add( elm.attribute( const_XPATH ).getStringValue() );
        }

        // Load Module Files
        FilesList = document.selectNodes( XPath_Module_Files );
        for ( Object aFilesList : FilesList )
        {
            Element elm = (Element) aFilesList;
            ModuleFilesList.add( elm.attribute( const_File ).getStringValue() );
        }

        // Load Modules XPaths
        FilesList = document.selectNodes( XPath_Module_XPaths );
        for ( Object aFilesList : FilesList )
        {
            Element elm = (Element) aFilesList;
            ModuleFilesXPathList.add( elm.attribute( const_XPATH ).getStringValue() );
        }

        // Load Installation Files
        FilesList = document.selectNodes( XPath_Inst_Files );
        for ( Object aFilesList : FilesList )
        {
            Element elm = (Element) aFilesList;
            CInstFile InstFile = new CInstFile();

            // Replace with variables values

            InstFile.SourceFile = elm.attribute( const_Name ).getStringValue();

            // Check for XPATH_COND existing
            if ( elm.attribute( const_XPATH_COND ) != null )
            {
                InstFile.XPath = elm.attribute( const_XPATH_COND ).getStringValue();
            } else
            {
                InstFile.XPath = "";
            }
            InstFiles.add( InstFile );
        }
        return res;
    }


    /**
     * Checks XPath Conditions on Files, and deletes unused Files from InstFiles
     */
    private static boolean CheckAllInstFileXPaths( ArrayList<CInstFile> InstFiles,
                                                   String ProgramConfigFileName,
                                                   ArrayList<String> ConfigFilesList
    )
    {
        ArrayList<CInstFile> resInstFiles = new ArrayList<>();

        System.out.println( "Checking for XPath conditions in [" + ProgramConfigFileName + "]" );

        // Use ProgramConfigFileName only for grabbing directory
        // Get Main Config File Dir
        StringBuffer ConfigDirName = new StringBuffer();
        FileUtils.DevideFilePathIntoParts( ProgramConfigFileName, ConfigDirName, null );

        // cycle on Config files
        for ( String aConfigFile : ConfigFilesList )
        {
            Document document;
            SAXReader oXmlReader;

            try
            {
                oXmlReader = new SAXReader();
                document = oXmlReader.read( new FileReader( ConfigDirName + File.separator + aConfigFile ) );
            } catch ( FileNotFoundException e )
            {
                System.err.println( "  ERROR: File [" + aConfigFile + "] NOT FOUND." );
                return false;
            } catch ( DocumentException e )
            {
                System.err.println( "  ERROR: File  [" + aConfigFile +
                        "] non-XML or CORRUPTED [" + e.getMessage() +
                        "]" );
                return false;
            }

            // Check all XPath File Conditions
            for ( Object InstFile : InstFiles )
            {
                CInstFile F = (CInstFile) InstFile;
                if ( ( F.XPath.equals( "" ) ) ||
                        ( ( !F.XPathFound ) && ( document.selectNodes( F.XPath ).size() > 0 ) ) )
                {
                    F.XPathFound = true;
                }
            }

        }

        // Save only found
        for ( CInstFile InstFile : InstFiles )
        {
            if ( InstFile.XPathFound ) {
                resInstFiles.add(InstFile);
                System.out.println("Found inst file: [" + InstFile.SourceFile + "] for Xpath [" + InstFile.XPath +  "]");
            }
        }

        // Copy found
        InstFiles.clear();
        InstFiles.addAll( resInstFiles );

        return ( true );
    }


    private static boolean FindAllNeedableInstFiles( ArrayList<CInstFile> InstFiles,
                                                     String ProgramConfigFileName,
                                                     ArrayList<String> OutResultInstFiles,
                                                     IVariablesStorage VarStorage )
    {

        // Use ProgramConfigFileName only for grabbing directory

        // Get Main Config File Dir
        StringBuffer ConfigDirName = new StringBuffer();
        FileUtils.DevideFilePathIntoParts( ProgramConfigFileName, ConfigDirName, null );

        // Create Filter Mask
        FileMaskFilenameFilter FNFilter = new FileMaskFilenameFilter( "" );

        // Process all Inst files
        for ( CInstFile InstFile : InstFiles )
        {

            // Create Inst Files Directory
            StringBuffer FileDir = new StringBuffer();
            StringBuffer FileName = new StringBuffer();
            FileUtils.DevideFilePathIntoParts( VarStorage.ReplaceWithVariableValues( InstFile.SourceFile ), FileDir, FileName );
            System.out.println("Searching packages in [" + FileDir.toString() + "]");
            File RootFileDirectory = new File( FileDir.toString() );

            // Set File Name (realy Regular expression)
            FNFilter.SetFileNameMask( FileName.toString() );

            // Get File Namei
            System.out.println("Searching [" + FileName.toString() + "] in [" + FileDir.toString() + "]");
            String[] lstFiles = RootFileDirectory.list( FNFilter );

            if ( ( lstFiles == null ) || ( lstFiles.length == 0 ) )
            {
                System.err.println( "  ERROR: Input File [" + InstFile.SourceFile + "] not found!" );
                return ( false );
            } else
            {
                // Cycle on all listed files
                for ( String lstFile : lstFiles )
                {
                    OutResultInstFiles.add( FileDir + File.separator + lstFile );
                }
            }
        }

        return ( true );
    }


    private static void MoveInstFilesToDistributionPlace( ArrayList<String> OutResultInstFiles,
                                                          String OutPutDir ) throws IOException
    {
        System.out.println( "Copying files:" );
        for ( String OutResultInstFile : OutResultInstFiles )
        {
            // FileUtils.CopyFileToDir( OutResultInstFile, OutPutDir );
            Path source = Paths.get( OutResultInstFile );
            Files.createDirectories( Paths.get( OutPutDir ) );
            Files.copy( source, Paths.get( OutPutDir + File.separator + source.getFileName() ) , REPLACE_EXISTING );
            System.out.println( "  Copy '" + OutResultInstFile + "' --> '" + OutPutDir + "'" );
        }
    }


    /**
     * Loads all information from RPM files and puts it in to PKGList
     */
    public static boolean LoadAllRPMInformation( String PKGDir, ArrayList<CRPMModule> PKGList )
    {
        String SystemOS = System.getProperty( "os.name" );
        if ( SystemOS.toLowerCase().contains( "windows" ) )
        {
            System.out.println( "Windows system detected (" + SystemOS + ")!" );
            return ( LoadAllRPMInformation_Windows( PKGDir, PKGList ) );
        } else
        {
            System.out.println( "NON-Windows system detected (" + SystemOS + ")!" );
            return ( LoadAllRPMInformation_UNIX( PKGDir, PKGList ) );
        }
    }

    /**
     * Loads all information from RPM files and puts it in to PKGList
     */
    public static boolean LoadAllRPMInformation_UNIX( String PKGDir, ArrayList<CRPMModule> PKGList )
    {
        FileMaskFilenameFilter FNFilter = new FileMaskFilenameFilter( ".*\\.rpm$" );
        File RootFileDirectory = new File( PKGDir );

        System.out.println( "Loading RPMs Information..." );

        // Get RPM File Names
        String[] lstFiles = RootFileDirectory.list( FNFilter );

        if ( ( lstFiles == null ) || ( lstFiles.length == 0 ) )
        {
            System.out.println( "  WARNING: RPM Files in directory [" + PKGDir + "] not found!" );
            return ( true );
        } else
        {

            // Cycle on all listed files
            for ( String lstFile : lstFiles )
            {
                CRPMModule RPM = new CRPMModule();
                RPM.FileName = PKGDir + File.separator + lstFile;
                try
                {
                    // Get Package Name
                    InputStream is3 = Runtime.getRuntime().exec( "rpm -qp --queryformat %{NAME} " + RPM.FileName ).getInputStream();
                    RPM.PackageName = FileUtils.InputStreamToString( is3 );

                    // Get List of File in RPM
                    InputStream is1 = Runtime.getRuntime().exec( "rpm -qpl " + RPM.FileName ).getInputStream();
                    RPM.FileConsistList = FileUtils.InputStreamToString( is1 );

                    // Get Dependency List of RPM
                    InputStream is2 = Runtime.getRuntime().exec( "rpm -qp --requires " + RPM.FileName ).getInputStream();
                    RPM.DependencyList = FileUtils.InputStreamToString( is2 );

                    System.out.println( "  Package [" + RPM.PackageName + "] loaded" );
                    //System.out.println("  Dependency ["+RPM.DependencyList+"]");
                    //System.out.println("  FileList ["+RPM.FileConsistList+"]");
                } catch ( IOException ex )
                {
                    System.err.println( "  ERROR: Error during outer program execution [" + ex.getMessage() + "]" );
                    return ( false );
                }

                PKGList.add( RPM );
            }
        }
        return ( true );
    }

    /**
     * Loads all information from RPM files and puts it in to PKGList
     */
    public static boolean LoadAllRPMInformation_Windows( String PKGDir, ArrayList<CRPMModule> PKGList )
    {
        FileMaskFilenameFilter FNFilter = new FileMaskFilenameFilter( ".*\\.rpm$" );
        File RootFileDirectory = new File( PKGDir );

        System.out.println( "Loading RPMs Information..." );

        // Get RPM File Names
        String[] lstFiles = RootFileDirectory.list( FNFilter );

        if ( ( lstFiles == null ) || ( lstFiles.length == 0 ) )
        {
            System.err.println( "  ERROR: RPM Files in directory [" + PKGDir + "] not found!" );
            return ( false );
        } else
        {

            // Cycle on all listed files
            for ( String lstFile : lstFiles )
            {
                CRPMModule RPM = new CRPMModule();
                RPM.FileName = PKGDir + File.separator + lstFile;

                try
                {
                    RPMFile rpmFile = new RPMFile( RPM.FileName );

                    // Get Package Name
                    RPM.PackageName = rpmFile.GetRPMPackageName();

                    // Get List of File in RPM
                    RPM.FileConsistList = stringutils.stringArrayToString( rpmFile.GetPKGListOfFile() );

                    // Get Dependency List of RPM
                    RPM.DependencyList = stringutils.stringArrayToString( rpmFile.GetRPMRequireName() );

                    System.out.println( "  Package [" + RPM.PackageName + "] loaded" );
                    Logger.global.info( "  Dependency [" + RPM.DependencyList + "]" );
                    Logger.global.info( "  FileList [" + RPM.FileConsistList + "]" );
                } catch ( Exception ex )
                {
                    System.err.println( "  ERROR: Error during reading RPM information [" + ex.getMessage() + "]" );
                    return ( false );
                }

                PKGList.add( RPM );
            }
        }
        return ( true );
    }


    public static boolean FindAllNecessaryRPMs( ArrayList<CRPMModule> PKGList,
                                                ArrayList<String> AllFilesList,
                                                ArrayList<CRPMModule> OutPKGList,
                                                ArrayList<String> OutResultInstFiles )
    {
        int iOldResultFilesCount;

        OutPKGList.clear();

        System.out.println( "Looking for RPMs dependencies..." );

        // Add files already Included in previuos steps (tag <FILE> in Profile)
        System.out.println( "  Profile Included files:" );
        for ( Object OutResultInstFile : OutResultInstFiles )
        {
            for ( CRPMModule RPMModule : PKGList )
            {
                if ( RPMModule.FileName.equals( OutResultInstFile ) )
                {
                    OutPKGList.add( RPMModule );
                    System.out.println( "    Package: [" + RPMModule.PackageName + "]" );
                }
            }
        }

        System.out.println( "  Direct File Dependencies:" );
        // First Step: Find necessary RPMs (check for consisting our files)
        for ( CRPMModule RPMModule : PKGList )
        {
            for ( String aFile : AllFilesList )
            {
                if ( RPMModule.FileConsistList.contains( aFile ) )
                {
                    //RPMModule.IsIncluded = true;
                    OutPKGList.add( RPMModule );
                    System.out.println( "    Package: [" + RPMModule.PackageName + "]" );
                    break;
                }
            }
        }

        // Step 2: Now check Package dependencies
        System.out.println( "  Package Dependencies:" );
        do
        {
            iOldResultFilesCount = OutPKGList.size();

            for ( int i = 0; i < OutPKGList.size(); i++ )
            {
                CRPMModule outRPMModule = OutPKGList.get( i );
                for ( CRPMModule RPMModule : PKGList )
                {
                    if ( OutPKGList.contains( RPMModule ) )
                    {
                        continue;
                    }
                    if ( outRPMModule.DependencyList.contains( RPMModule.PackageName ) )
                    {
                        //RPMModule.IsIncluded=true;
                        OutPKGList.add( RPMModule );
                        System.out.println( "    [" + outRPMModule.PackageName + "] --> [" + RPMModule.PackageName + "]" );
                    }
                }
            }

            // Check that we added new outer files
        }
        while ( OutPKGList.size() != iOldResultFilesCount );

        System.out.println( "  Resulting Packages:" );
        // Step 3: Now makes Real File Name List
        for ( CRPMModule resRPM : OutPKGList )
        {
            OutResultInstFiles.add( resRPM.FileName );
            System.out.println( "    Package: [" + resRPM.PackageName + "]" );
        }
        return ( true );
    }


    // Check that ALL module files exist in given RPM List.
    // It's nessecary for correct distribution building
    // Returns OutNotIncludedModuleFiles
    public static boolean CheckForNotIcludedModuleFiles(
            ArrayList<CRPMModule> PKGList,
            ArrayList<String> ModuleFilesList,
            SortedSet<String> OutNotIncludedModuleFiles )
    {
        OutNotIncludedModuleFiles.clear();

        // Check for including in RPM
        for ( String strModuleFileName : ModuleFilesList )
        {
            boolean bIsIncluded = false;
            int j = 0;

            while ( ( j < PKGList.size() ) && ( !bIsIncluded ) )
            {
                CRPMModule RPMModule = PKGList.get( j );
                bIsIncluded = ( RPMModule.FileConsistList.contains( strModuleFileName ) );
                j++;
            }

            // if not included, add it!
            if ( !bIsIncluded )
            {
                OutNotIncludedModuleFiles.add( strModuleFileName );
            }
        }

        return ( OutNotIncludedModuleFiles.isEmpty() );
    }


    private static boolean CheckAbsentModuleFilesInClientConfigurationFiles(
            String ProgramConfigFileName,
            SortedSet<String> AbsentModuleFileList,
            SortedSet<String> OutNotIncludedModuleFiles )
    {
        if ( AbsentModuleFileList.size() == 0 )
        {
            return true;
        }

        OutNotIncludedModuleFiles.clear();

        // Get Main Config File Dir
        StringBuffer DirName = new StringBuffer();
        FileUtils.DevideFilePathIntoParts( ProgramConfigFileName, DirName, null );

        // Check for all files in configuration
        for ( String strModuleFileName : AbsentModuleFileList )
        {
            File file = new File( DirName + File.separator + strModuleFileName );
            if ( !file.exists() )
            {
                OutNotIncludedModuleFiles.add( strModuleFileName );
            } else
            {
                System.out.println( "  WARNING: Module File [" + strModuleFileName + "] Included As Configuration File! (taken from client configuration)" );
            }
        }

        // Main cycle on File List
        for ( String strModuleFileName : OutNotIncludedModuleFiles )
        {
            // Get File Name
            System.err.println( "  FATAL ERROR: Module File [" + strModuleFileName + "] Not Included in OUT RPMS Nor As Configuration File!" );
        }

        return ( OutNotIncludedModuleFiles.isEmpty() );
    }


    public static void main( String[] args ) throws Exception
    {
        boolean res;
        System.out.println("Arguments:");
        for( String arg : args ) { System.out.println(arg); };

        Logger.global.setLogLevel( Logger.LOG_LEVEL_DEBUG );

        if ( args.length < 2 )
        {
            PrintHelp();
            System.exit( 0 );
        }

        ArrayList<CRPMModule> AllPKGList = new ArrayList<>();

        ArrayList<CInstFile> InstFiles = new ArrayList<>();
        ArrayList<String> ResultingInstFiles = new ArrayList<>();
        ArrayList<String> ConfigFilesXPathList = new ArrayList<>();
        ArrayList<String> ConfigFilesList = new ArrayList<>();
        ArrayList<String> ModuleFilesXPathList = new ArrayList<>();
        ArrayList<String> ModuleFilesList = new ArrayList<>();
        ArrayList<CRPMModule> OutPKGList = new ArrayList<>();
        SortedSet<String> NotIncludedModuleFiles = new TreeSet<>( Collator.getInstance() );
        SortedSet<String> NotIncludedModuleFilesAsConfigFiles = new TreeSet<>( Collator.getInstance() );

        IVariablesStorage VariablesStorage = new VariablesStorage();

        // Set Escape and Variable Characters
        VariablesStorage.SetControlCharacters( '\\', '$' );

        // Set Command line arguments
        VariablesStorage.SetCommandLineArgs( args );

        //Load Config files
        System.out.println( "Loading main config files starting from [" + args[ 0 ] + "]");

        res = LoadConfigFile( args[ 0 ],
                VariablesStorage,
                InstFiles,
                ConfigFilesXPathList,
                ConfigFilesList,
                ModuleFilesXPathList,
                ModuleFilesList );

        // set special PKGDIR variable
        if ( args.length >= 3 ) {
            VariablesStorage.SetVariableValue("PKGDIR", args[2]);
            System.out.println("Package directory specified in call arguments; replacing default value with [" +
                    args[2] + "]");
        }


        // ----------------------------------- First Work Part ---------------------------
        // looking for files from <FILES> section and checking corresponding XPath conditions

        // Check XPath conditions
        res = res && CheckAllInstFileXPaths( InstFiles, args[ 0 ], ConfigFilesList );


        // Find All needable Files
        res = res && FindAllNeedableInstFiles( InstFiles, args[ 0 ], ResultingInstFiles, VariablesStorage );

        // ------------------------ Second Work Part -------------------------------
        // Checking for included files in Client MultiChannelServer Configuration and
        // checking RPM files for this files.

        // long l = System.currentTimeMillis();

        System.out.println( "Searching for included config files..." );
        // Recursevly Find All config files
        ArrayList<String> newConfigFilesList = mainPool.invoke( new FindAllFilesFromClientConfigurationTask( args[ 0 ], ConfigFilesXPathList, ConfigFilesList,
                ConfigFilesXPathList, "*.xml" ) );

//        System.out.println( System.currentTimeMillis() - l  );

        if ( newConfigFilesList == null )
            res = false;
        else
            ConfigFilesList.addAll( newConfigFilesList );

        // We devide into two tasks becasuse we use new ConfigFilesList in searching of modules
        System.out.println( "Searching for included module files..." );
        // Recursevly Find All module files
        ModuleFilesList = mainPool.invoke( new FindAllFilesFromClientConfigurationTask( args[ 0 ], ConfigFilesXPathList, ConfigFilesList,
                ModuleFilesXPathList, "*.*" ) );

//        System.out.println( System.currentTimeMillis() - l  );

        res = res && ( ModuleFilesList != null );

        // Load all RPM Information
        res = res && LoadAllRPMInformation( VariablesStorage.GetVariableValue( const_PKGDIR_Variable ), AllPKGList );

        // All Files = Config Files + Module Files
        ArrayList<String> AllFileList = new ArrayList<>( ConfigFilesList );
        AllFileList.addAll( ModuleFilesList );

        // Find all necessary RPMs
        res = res && FindAllNecessaryRPMs( AllPKGList, AllFileList, OutPKGList, ResultingInstFiles );

        if ( !res )
        {
            System.out.println( "Distribution FAILED! See Error file for details!" );
            System.exit( 1 );
        }

        // Check that all module files exist in output packages
        System.out.println( "Check that all module files exist in output packages ..." );
        boolean bFinalCheck = CheckForNotIcludedModuleFiles( OutPKGList,
                ModuleFilesList,
                NotIncludedModuleFiles );

        // Check that non existing module files exist as client configuration files ...
        System.out.println(
                "Check that non existing module files exist as client configuration files ..." );
        if ( !bFinalCheck )
        {
            bFinalCheck = CheckAbsentModuleFilesInClientConfigurationFiles( args[ 0 ],
                    NotIncludedModuleFiles,
                    NotIncludedModuleFilesAsConfigFiles );
        }

        // if all checks passed, Copy Files!
        if ( bFinalCheck )
        {
            // Move Files
            MoveInstFilesToDistributionPlace( ResultingInstFiles, args[ 1 ] );
        } else
        {
            System.out.println( "Distribution FAILED! See Error file for details!" );
            System.exit( 1 );
        }
    }
}
