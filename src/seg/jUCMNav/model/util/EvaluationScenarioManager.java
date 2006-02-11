/**
 * 
 */
package seg.jUCMNav.model.util;

import grl.Decomposition;
import grl.DecompositionType;
import grl.Dependency;
import grl.ElementLink;
import grl.Evaluation;
import grl.EvaluationScenario;
import grl.GRLspec;
import grl.IntentionalElement;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import seg.jUCMNav.model.ModelCreationFactory;
import seg.jUCMNav.model.commands.create.AddEvaluationCommand;

/**
 * This class is a singleton responsible to manage the current scenario. 
 * It does the evaluation calculation for IntentionalElement, create the Evaluation
 * and return the value of the evaluation given an IntentionalElement for the current scenario.
 * 
 * @author Jean-Fran�ois Roy
 *
 */
public class EvaluationScenarioManager {
    
    private EvaluationScenario scenario;
    private static EvaluationScenarioManager instance;
    private HashMap evaluations; //HashMap to keep link between intentionalElement and the evaluation for a particular scenario
    
    private class EvaluationCalculation{
        public IntentionalElement element;
        public int linkCalc;
        public int totalLinkDest;
        
        public EvaluationCalculation(IntentionalElement element, int totalLink){
            this.element = element;
            this.totalLinkDest = totalLink;
            linkCalc = 0;
        }
    }
    /**
     * 
     */
    private EvaluationScenarioManager() {
        
    }

    public static synchronized EvaluationScenarioManager getInstance()
    {
        if (instance == null){
            instance = new EvaluationScenarioManager();
        }
        return instance;
    }
    
    public EvaluationScenario getEvaluationScenario(){
        return scenario;
    }
    
    public void setEvaluationScenario(EvaluationScenario scen){
        scenario = scen;
        
        //Create a new hash map for this scenario
        evaluations = new HashMap();
        //Go through all the intentionalElement and create a new Evaluation object if no one exist for this scenario
        GRLspec grl = scenario.getGrlspec();
        Iterator it = grl.getIntElements().iterator();
        while (it.hasNext()){
            IntentionalElement elem = (IntentionalElement)it.next();
            //Verify if an evaluation exist for this scenario. This could create performance problem!!!!
            Iterator sc = scenario.getEvaluations().iterator();
            Evaluation eval = null;
            while(sc.hasNext() && eval == null){
                Evaluation temp = (Evaluation)sc.next();
                if (temp.getIntElement() == elem){
                    eval = temp;
                }
            }
            if (eval == null){
                eval = (Evaluation)ModelCreationFactory.getNewObject(grl.getUrnspec(), Evaluation.class);
            }
            evaluations.put(elem,eval);
        }
        calculateEvaluation();
    }
    
    public Evaluation getEvaluationObject(IntentionalElement elem){
        return (Evaluation)evaluations.get(elem);
    }
    
    public int getEvaluation(IntentionalElement elem){
        Evaluation temp = (Evaluation)evaluations.get(elem);
        return temp.getEvaluation();
    }
    
    public void setIntentionalElementEvaluation(IntentionalElement element, int value){
        //The evaluation could only be between 100 and -100. Do nothing if it is not the case
        if (value<=100 && value>=-100){
            Evaluation eval = (Evaluation)evaluations.get(element);
            //Change the value in the evaluation
            if (value != eval.getEvaluation()){
                eval.setEvaluation(value);
            }
            //If it is a new Evaluation enter by the user, link it with the scenario and intentionalElement
            AddEvaluationCommand cmd = new AddEvaluationCommand(eval, element, scenario);
            if (cmd.canExecute()){
                cmd.execute();
            }
            //Calculate the new evaluations
            calculateEvaluation();
        }
    }
    
    private void calculateEvaluation(){
        Vector evalReady = new Vector();
        HashMap evaluationCalculation = new HashMap();
        
        //We need to go through the list of IntentionalElement. If it is a leaf node, or if it has a Evaluation 
        //link, it is ready to be calculate.
        
        Iterator it = scenario.getGrlspec().getIntElements().iterator();
        while (it.hasNext()){
            IntentionalElement element = (IntentionalElement)it.next();
            if (element.getLinksDest().size() == 0 || ((Evaluation)evaluations.get(element)).getScenario() != null){
                evalReady.add(element);
            } else{
                EvaluationCalculation calculation = new EvaluationCalculation(element, element.getLinksDest().size());
                evaluationCalculation.put(element,calculation);
            }
        }
        
        while(evalReady.size() > 0){ //We loop until we empty this list
            IntentionalElement intElem = (IntentionalElement)evalReady.remove(0);
            calculateElementEvaluation(intElem);
            //TODO Find how to generate notification to intentionalElement here
            //This code is a big hack that should be clean up!!!!!
            intElem.setName(intElem.getName());
            
            for (Iterator j=intElem.getLinksSrc().iterator();j.hasNext();){
                IntentionalElement temp = ((ElementLink)j.next()).getDest();
                if (evaluationCalculation.containsKey(temp)){
                    EvaluationCalculation calc = (EvaluationCalculation)evaluationCalculation.get(temp);
                    calc.linkCalc += 1;
                    if (calc.linkCalc >= calc.totalLinkDest){
                        evaluationCalculation.remove(temp);
                        evalReady.add(calc.element);
                    }
                }
            }
        }
    }
    
    private void calculateElementEvaluation(IntentionalElement element){
        Evaluation eval = (Evaluation)evaluations.get(element);
        if (element.getLinksDest().size() == 0){
            return;
        }
        int result = eval.getEvaluation();
        int decompositionValue = -10000;
        int dependencyValue = -10000;
        
        Iterator it = element.getLinksDest().iterator(); //Return the list of elementlink
        while (it.hasNext()){
            ElementLink link = (ElementLink)it.next();
            if (link instanceof Decomposition){
                if (decompositionValue < -100){
                    decompositionValue = ((Evaluation)evaluations.get(link.getSrc())).getEvaluation();
                } else if (element.getDecompositionType().getValue() == DecompositionType.AND){
                    if (decompositionValue > ((Evaluation)evaluations.get(link.getSrc())).getEvaluation()){
                        decompositionValue = ((Evaluation)evaluations.get(link.getSrc())).getEvaluation();
                    }
                } else if (element.getDecompositionType().getValue() == DecompositionType.OR){
                    if (decompositionValue < ((Evaluation)evaluations.get(link.getSrc())).getEvaluation()){
                        decompositionValue = ((Evaluation)evaluations.get(link.getSrc())).getEvaluation();
                    }
                } 
            } else if (link instanceof Dependency){
                if (dependencyValue < ((Evaluation)evaluations.get(link.getSrc())).getEvaluation()){
                    dependencyValue = ((Evaluation)evaluations.get(link.getSrc())).getEvaluation();
                }
            }
        }
        if (decompositionValue >=-100){
            result = decompositionValue;
        }
        if ((dependencyValue >= -100) && (result > dependencyValue)){
            result = dependencyValue;
        }
        eval.setEvaluation(result);
    }
}