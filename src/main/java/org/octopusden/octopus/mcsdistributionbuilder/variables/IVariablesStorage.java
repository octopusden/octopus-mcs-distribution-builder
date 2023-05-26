package org.octopusden.octopus.mcsdistributionbuilder.variables;

@SuppressWarnings({ "JavaDoc" })
public interface IVariablesStorage {
    void SetControlCharacters( char EscapeCharater, char VariableCharacter );


    /**
     * Get Variable Value. It's already solved related to another variables.
     * For example if "$PATH=$ROOT_$FILEPATH"
     */
    String GetVariableValue( String VariableName );


    /**
     * Set Variable Name, if it doesn't exist create it.
     *
     * @param VariableName  Variable Name
     * @param VariableValue Variable Value
     */
    void SetVariableValue( String VariableName, String VariableValue );


    /**
     * Replaces variables inside InText
     */
    String ReplaceWithVariableValues( String InText );


    /**
     * Set Inner variables for command line string.
     * For Example sets variables $1,$2,$3 if Escape Characted = '$'
     */
    void SetCommandLineArgs( String args[] );
}
