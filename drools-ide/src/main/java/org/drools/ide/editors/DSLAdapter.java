package org.drools.ide.editors;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.drools.ide.editors.completion.RuleCompletionProposal;
import org.drools.lang.dsl.template.NLGrammar;
import org.drools.lang.dsl.template.NLMappingItem;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.part.FileEditorInput;

/**
 * This holds the DSL configuration for an editor instance.
 * When loading, it will find the DSL file, and load the applicable lists.
 * 
 * This provides a link between the editor and the DSL features of the rule language.  
 * 
 * It will look for a DSL configuration, as named in the rule file, in the same directory as the rule file.
 * Failing this, it will search one directory above the rule file. 
 * Failing that, it will search the root of the project in the workspace.
 * 
 * @author Michael Neale
 */
public class DSLAdapter {

    private String dslConfigName;
    private boolean valid = false;
    private List conditionProposals = new ArrayList();
    private List consequenceProposals = new ArrayList();
    
    
    //to dig out the expander, without using the parser.
    private static final Pattern expander = Pattern.compile( "^.*expander\\s*(.*)\\.dsl.*", 
                                                             Pattern.DOTALL | Pattern.MULTILINE );
    /**
     * This will sniff out the DSL config file name from the content.
     * It will then use the IFile input to search around for the file itself.
     * TODO: provide an alternative that just loads off a stream (for non IDEs workbenches like jlibrary).
     * @param content Rule source
     * @param input File from the FileEditorInput
     */
    public DSLAdapter(String content, IFile input) {
        
        dslConfigName = findDSLConfigName( content );
        if (dslConfigName == null) return;
        loadConfig( input );
    }
    
    /** Get a reader to the DSL contents */
    public static Reader getDSLContent(String ruleSource, IFile input) throws CoreException {
        String dslFileName = findDSLConfigName( ruleSource );
        if (dslFileName == null) return null;
        IResource res = findDSLResource( input, dslFileName );
        if (res instanceof IFile) {
            IFile dslConf = (IFile) res;
            if (dslConf.exists()) {
                return new InputStreamReader(dslConf.getContents());
            }
        }
        return null;
    }

    /**
     * This does the hunting around the projec to find the .dsl file.
     */
    private void loadConfig(IFile input) {
        IResource res = findDSLResource( input, dslConfigName );
        if (res instanceof IFile) {
            IFile dslConf = (IFile) res;
            if (dslConf.exists()) {
                InputStream stream = null; 
                try {
                    stream = dslConf.getContents();
                    readConfig( stream );
                    valid = true;
                } catch ( Exception e ) {
                    throw new IllegalStateException("Unable to open DSL config file. (Exception: " + e.getMessage() + ")");
                } finally {
                    closeStream( stream );
                }
                
            }
        }
    }

    private static IResource findDSLResource(IFile input, String dslFileName) {
        IResource res = input.getParent().findMember( dslFileName );
        if (res == null) res = input.getParent().getParent().findMember( dslFileName ); //try parent directory
        if (res == null) res = input.getProject().findMember( dslFileName ); //try root of project.
        return res;
    }

    
    /** This will load in the DSL config file, using the NLGrammar from drools-compiler */
    void readConfig(InputStream stream) throws IOException, CoreException {
        NLGrammar grammar = new NLGrammar();
        grammar.load( new InputStreamReader(stream) );

        List conditions = grammar.getMappings( "when" );
        List consequences = grammar.getMappings( "then" );
        
        conditionProposals = new ArrayList(conditions.size());
        consequenceProposals = new ArrayList(consequences.size());
        
        buildProposals( conditions, conditionProposals );
        buildProposals( consequences, consequenceProposals );

    }

    private void buildProposals(List suggestions, List proposals) {
        for ( Iterator iter = suggestions.iterator(); iter.hasNext(); ) {
            NLMappingItem text = (NLMappingItem) iter.next();
            RuleCompletionProposal proposal = new RuleCompletionProposal(text.getNaturalTemplate());
            proposals.add(proposal);
        }
    }

    private void closeStream(InputStream stream) {
        if (stream != null) try {
            stream.close();
        } catch ( IOException e ) {}
    }

    DSLAdapter() {
        
    }

    /** Sniffs out the expander/DSL config name as best it can. */
    static String findDSLConfigName(String content) {
        String name = null;
        Matcher matches = expander.matcher( content );
        if (matches.matches()) {
            name = matches.group(1) + ".dsl";
        }
        return name;
    }
    
    
    String getDSLConfigName() {
        return dslConfigName;
    }
    
    
    public boolean isValid() {
        return valid;
    }
    
    
    public boolean hasConditions() {
        return conditionProposals.size() > 0;
    }
    
    public boolean hasConsequences() {
        return consequenceProposals.size() > 0;
    }
    
    public List listConditionItems() {
        return conditionProposals;
    }
    
    public List listConsequenceItems() {
        return consequenceProposals;
    }    
    
}
