package org.octopusden.octopus.mcsdistributionbuilder;

import org.octopusden.octopus.mcsdistributionbuilder.fileutils.FileUtils;
import org.octopusden.octopus.mcsdistributionbuilder.logutils.Logger;
import org.octopusden.octopus.mcsdistributionbuilder.stringutils.stringutils;

import java.io.*;

@SuppressWarnings({ "JavaDoc" })
public class RPMFile {
    // signature
    RPMHeaderStructure signature;
    RPMHeaderStructure header;

    // Tags
    public static final int CONST_RPMTAG_NAME = 1000;
    public static final int CONST_RPMTAG_FILENAMES = 1027;
    public static final int CONST_RPMTAG_REQUIRENAME = 1049;


    // Discovered by hand
    public static final int CONST_RPMTAG_DIRINDEXES = 1116;
    public static final int CONST_RPMTAG_BASENAMES = 1117;
    public static final int CONST_RPMTAG_DIRNAMES = 1118;

    public RPMFile( String sFileName ) throws IOException
    {
        BufferedInputStream bufferedInputStream = new BufferedInputStream( new FileInputStream( sFileName ), 4092 );

        // Read Lead
        byte[] bufLead = new byte[ 0x60 ];
        bufferedInputStream.read( bufLead );
        // Read Signature
        Logger.global.debug( "Read Signature" );
        signature = new RPMHeaderStructure( bufferedInputStream );

        // Read Header
        Logger.global.debug( "Read Header" );
        header = new RPMHeaderStructure( bufferedInputStream );

        bufferedInputStream.close();
    }

    private RPMHeaderStructure_Tag FindTag( int iRPMTag )
    {
        RPMHeaderStructure_Tag res = null;

        for ( int i = 0; i < header.Tags.length; i++ )
        {
            if ( header.Tags[ i ].iTag == iRPMTag )
            {
                res = header.Tags[ i ];
                break;
            }
        }
        return ( res );
    }

    public String GetRPMTagStringValue( int iRPMTag )
    {
        String res = "";
        RPMHeaderStructure_Tag RPMTag = FindTag( iRPMTag );
        if ( ( RPMTag != null ) && ( RPMTag.iFormat == RPMHeaderStructure_Tag.CONST_RPMTAG_FORMAT_STRING ) )
        {
            res = RPMTag.strValue;
        }

        return ( res );
    }

    public String[] GetRPMTagStringArrayValue( int iRPMTag )
    {
        String res[] = null;
        RPMHeaderStructure_Tag RPMTag = FindTag( iRPMTag );
        if ( ( RPMTag != null ) &&
                ( ( RPMTag.iFormat == RPMHeaderStructure_Tag.CONST_RPMTAG_FORMAT_STRING_ARRAY ) ||
                        ( RPMTag.iFormat == RPMHeaderStructure_Tag.CONST_RPMTAG_FORMAT_I18NSTRING_TYPE )
                )
                )
            res = RPMTag.arrStrValue;

        return ( res );
    }

    public long[] GetRPMTagLongArrayValue( int iRPMTag )
    {
        long res[] = null;
        RPMHeaderStructure_Tag RPMTag = FindTag( iRPMTag );
        if ( ( RPMTag != null ) &&
                ( ( RPMTag.iFormat == RPMHeaderStructure_Tag.CONST_RPMTAG_FORMAT_CHAR ) ||
                        ( RPMTag.iFormat == RPMHeaderStructure_Tag.CONST_RPMTAG_FORMAT_INT8 ) ||
                        ( RPMTag.iFormat == RPMHeaderStructure_Tag.CONST_RPMTAG_FORMAT_INT16 ) ||
                        ( RPMTag.iFormat == RPMHeaderStructure_Tag.CONST_RPMTAG_FORMAT_INT32 ) ||
                        ( RPMTag.iFormat == RPMHeaderStructure_Tag.CONST_RPMTAG_FORMAT_INT64 )
                )
                )
            res = RPMTag.arrLongValue;

        return ( res );
    }


    public String GetRPMPackageName()
    {
        return ( GetRPMTagStringValue( CONST_RPMTAG_NAME ) );
    }

    public String[] GetPKGListOfFile()
    {
        // Old RPM Format
        String res[] = GetRPMTagStringArrayValue( CONST_RPMTAG_FILENAMES );

        // New Rpm Format
        if ( res == null )
        {
            long FileListFilesToDir[] = GetRPMTagLongArrayValue( CONST_RPMTAG_DIRINDEXES );
            String FileListNames[] = GetRPMTagStringArrayValue( CONST_RPMTAG_BASENAMES );
            String FileListFolders[] = GetRPMTagStringArrayValue( CONST_RPMTAG_DIRNAMES );
            if ( ( FileListNames != null ) && ( FileListFolders != null ) && ( FileListFilesToDir != null ) )
            {
                res = new String[ FileListNames.length ];
                for ( int i = 0; i < res.length; i++ )
                    res[ i ] = FileListFolders[ ( (int) FileListFilesToDir[ i ] ) ] + FileListNames[ i ];
            }
        }

        return ( res );
    }

    public String[] GetRPMRequireName()
    {
        return ( GetRPMTagStringArrayValue( CONST_RPMTAG_REQUIRENAME ) );
    }


    public class RPMHeaderStructure {
        private byte bufMagic[] = new byte[ 3 ];
        private byte bufStore[];
        private byte bVersion;
        private int iIndexCount;
        private int iStoreSize;
        private RPMHeaderStructure_Tag Tags[];

        public RPMHeaderStructure( InputStream inputStream ) throws IOException
        {

            DataInputStream dataInputStream = new DataInputStream( inputStream );

            // Read Header Structure Header
            inputStream.read( bufMagic );
            bVersion = dataInputStream.readByte();
            dataInputStream.readInt(); // reserved for future use
            iIndexCount = dataInputStream.readInt();
            iStoreSize = dataInputStream.readInt();

            Logger.global.debug( "Found Header structure! Version:" + bVersion + " IndexCount:" + iIndexCount + " Store size:" + iStoreSize );

            // Init Tags
            Tags = new RPMHeaderStructure_Tag[ iIndexCount ];

            // read Tags
            Logger.global.debug( "Read Tags!" );
            for ( int i = 0; i < Tags.length; i++ )
            {
                Tags[ i ] = new RPMHeaderStructure_Tag( inputStream );
            }

            // Read Tags Values from Store
            Logger.global.debug( "Read Tags Values from Store!" );
            // Really Store Size round to 8-byte roundary
            if ( iStoreSize % 8 != 0 )
                iStoreSize = ( ( iStoreSize / 8 ) + 1 ) * 8;

            bufStore = new byte[ iStoreSize ];
            inputStream.read( bufStore );

            for ( RPMHeaderStructure_Tag Tag : Tags ) Tag.ReadValueFromStore( bufStore );
        }


    }

    public class RPMHeaderStructure_Tag {


        public static final int CONST_RPMTAG_FORMAT_CHAR = 1;
        public static final int CONST_RPMTAG_FORMAT_INT8 = 2;
        public static final int CONST_RPMTAG_FORMAT_INT16 = 3;
        public static final int CONST_RPMTAG_FORMAT_INT32 = 4;
        public static final int CONST_RPMTAG_FORMAT_INT64 = 5;
        public static final int CONST_RPMTAG_FORMAT_STRING = 6;
        public static final int CONST_RPMTAG_FORMAT_STRING_ARRAY = 8;
        public static final int CONST_RPMTAG_FORMAT_I18NSTRING_TYPE = 9;

        int iTag;
        int iFormat;
        int iOffset;
        int iCount;

        // values
        String strValue;
        long arrLongValue[]; // All numbes
        byte bufBinValue[];
        String arrStrValue[];

        /**
         * Read Index Entry from Stream (tag)
         *
         * @param fileInputStreamIndex
         */

        public RPMHeaderStructure_Tag( InputStream fileInputStreamIndex ) throws IOException
        {
            DataInputStream dataInputStream = new DataInputStream( fileInputStreamIndex );
            iTag = dataInputStream.readInt();
            iFormat = dataInputStream.readInt();
            iOffset = dataInputStream.readInt();
            iCount = dataInputStream.readInt();
            Logger.global.debug( "Found Tag:" + iTag + " Format:" + iFormat + " Offset:" + iOffset + " Count:" + iCount );
        }

        public void ReadValueFromStore( byte bufStore[] ) throws IOException
        {
            ByteArrayInputStream byteStream = new ByteArrayInputStream( bufStore );
            DataInputStream dataInputStream = new DataInputStream( byteStream );

            // Skip to Offset Position
            byteStream.skip( iOffset );

            switch ( iFormat )
            {
                // char,byte
                case 1:
                case 2:
                    arrLongValue = new long[ iCount ];
                    for ( int i = 0; i < arrLongValue.length; i++ )
                        arrLongValue[ i ] = dataInputStream.readByte();

                    Logger.global.debug( "   Found Tag Value:" + iTag + " Value: " + stringutils.longArrayToString( arrLongValue ) );
                    break;

                case 3:
                    arrLongValue = new long[ iCount ];
                    for ( int i = 0; i < arrLongValue.length; i++ )
                        arrLongValue[ i ] = dataInputStream.readShort();

                    Logger.global.debug( "   Found Tag Value:" + iTag + " Value: " + stringutils.longArrayToString( arrLongValue ) );
                    break;
                case 4:
                    arrLongValue = new long[ iCount ];
                    for ( int i = 0; i < arrLongValue.length; i++ )
                        arrLongValue[ i ] = dataInputStream.readInt();

                    Logger.global.debug( "   Found Tag Value:" + iTag + " Value: " + stringutils.longArrayToString( arrLongValue ) );
                    break;

                case 5:
                    arrLongValue = new long[ iCount ];
                    for ( int i = 0; i < arrLongValue.length; i++ )
                        arrLongValue[ i ] = dataInputStream.readLong();

                    Logger.global.debug( "   Found Tag Value:" + iTag + " Value: " + stringutils.longArrayToString( arrLongValue ) );
                    break;

                case 6:
                    strValue = FileUtils.InputStreamToString( byteStream ); //  dataInputStream.readLine();
                    Logger.global.debug( "   Found Tag Value:" + iTag + " Value: " + strValue );
                    break;

                case 7:
                    bufBinValue = new byte[ iCount ];
                    dataInputStream.readFully( bufBinValue );
                    Logger.global.debug( "   Found Tag Value:" + iTag + " Value: " + bufBinValue );
                    break;

                case 8:
                case 9:
                    arrStrValue = new String[ iCount ];
                    for ( int i = 0; i < iCount; i++ )
                        arrStrValue[ i ] = FileUtils.InputStreamToString( byteStream );

                    Logger.global.debug( "   Found ArrayList Tag :" + iTag + " Value:" + stringutils.stringArrayToString( arrStrValue ) );

                    break;
            }
        }
    }

}
