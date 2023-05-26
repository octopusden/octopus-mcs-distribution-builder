package org.octopusden.octopus.mcsdistributionbuilder.logutils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    public static final int LOG_LEVEL_ERROR = 0;
    public static final int LOG_LEVEL_WARNING = 1;
    public static final int LOG_LEVEL_INFO = 2;
    public static final int LOG_LEVEL_DEBUG = 3;


    public static Logger global = new Logger( LOG_LEVEL_ERROR );

    private int logLevel;
    private SimpleDateFormat dateFormat = new SimpleDateFormat( "HH:mm:ss" );

    public Logger( int logLevel )
    {
        setLogLevel( logLevel );
    }

    public void error( String msg )
    {
        log( LOG_LEVEL_ERROR, msg );
    }

    public void warning( String msg )
    {
        log( LOG_LEVEL_WARNING, msg );
    }

    public void info( String msg )
    {
        log( LOG_LEVEL_INFO, msg );
    }

    public void debug( String msg )
    {
        log( LOG_LEVEL_DEBUG, msg );
    }

    public void log( int logLevel, String msg )
    {
        if ( logLevel > this.logLevel ) return;

        String header = dateFormat.format( new Date() ) + " | ";
        switch ( logLevel )
        {
            case LOG_LEVEL_ERROR:
                System.err.println( header + "[error] " + msg );
                break;
            case LOG_LEVEL_WARNING:
                System.out.println( header + "[warning] " + msg );
                break;
            case LOG_LEVEL_INFO:
                System.out.println( header + "[info] " + msg );
                break;
            case LOG_LEVEL_DEBUG:
                System.out.println( header + "[debug] " + msg );
                break;
        }
    }

    public int getLogLevel()
    {
        return logLevel;
    }

    public void setLogLevel( int logLevel )
    {
        this.logLevel = logLevel;
    }
}
