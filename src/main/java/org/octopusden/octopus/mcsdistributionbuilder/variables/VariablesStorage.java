package org.octopusden.octopus.mcsdistributionbuilder.variables;


import java.util.HashMap;

public class VariablesStorage
        implements IVariablesStorage {
    private char EscapeChar;
    private char VariableChar;
    private HashMap<String, String> varMap = new HashMap<>();


    // Implements interface IVariablesStorage
    public void SetControlCharacters( char EscapeCharater, char VariableCharacter )
    {
        EscapeChar = EscapeCharater;
        VariableChar = VariableCharacter;
    }


    public String GetVariableValue( String VariableName )
    {
        //Looking for exist variables
        return ( varMap.containsKey( VariableName ) ? varMap.get( VariableName ) : "" );
    }


    public void SetVariableValue( String VariableName, String VariableValue )
    {
        System.out.println( "Arg: '" + VariableName + "' = [" + VariableValue + "]" );
        varMap.put( VariableName, ReplaceWithVariableValues( VariableValue ) );
    }


    public String ReplaceWithVariableValues( String InText )
    {
        String res = InText;

        // Start Replacing Variables
        for ( String varName : varMap.keySet() )
        {
            System.out.println("Replacing variable [" + varName + "], value [" + varMap.get(varName)+ "]");
            res = res.replaceAll( SearchStringToRegExp( VariableChar + varName ),
                    MaskSpecialCharsFromReplaceString( varMap.get( varName ) ) );
        }

        // Replace True Variable Characters. Ex: "\$"->"$"
        return ( res.replaceAll( "\\" + EscapeChar + "\\" + VariableChar, String.valueOf( VariableChar ) ) );
    }


    private String SearchStringToRegExp( String SearchString )
    {
        // Escape Char for reliability
        String SearchPat = "\\" + VariableChar;
        String EscPatt = "(?<=\\[\\^\\\\" + EscapeChar + "\\])?\\[\\" + VariableChar + "\\]";
        return SearchString.replaceAll( SearchPat, EscPatt );
    }

    private String MaskSpecialCharsFromReplaceString( String ReplaceString )
    {
        String res = ReplaceString;
        res = res.replaceAll( "\\\\", "\\\\\\\\" );
        res = res.replaceAll( "\\$", "\\$" );
        return ( res );
    }

    public void SetCommandLineArgs( String args[] )
    {
        // Add variables from command line
        for ( int i = 1; i < args.length; i++ )
        {
            SetVariableValue( VariableChar + String.valueOf( i ), args[ i ] );
        }
    }
}
