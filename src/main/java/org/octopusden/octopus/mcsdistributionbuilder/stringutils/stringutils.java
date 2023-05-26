package org.octopusden.octopus.mcsdistributionbuilder.stringutils;

public class stringutils {
    public static String stringArrayToString( String arr[] )
    {
        if ( arr == null ) return ( null );

        StringBuilder res = new StringBuilder();
        for ( int i = 0; i < arr.length; i++ )
            res.append( arr[ i ] ).append( ( i == arr.length - 1 ? "" : "\n" ) );

        return ( res.toString() );
    }

    public static String longArrayToString( long arr[] )
    {
        if ( arr == null ) return ( null );

        StringBuilder res = new StringBuilder( 100 );
        for ( int i = 0; i < arr.length; i++ )
            res.append( arr[ i ] ).append( ( i == arr.length - 1 ? "" : "\n" ) );

        return ( res.toString() );
    }
}
