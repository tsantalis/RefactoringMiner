package org.openhab.ui.habot.test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.junit.Test;
import org.openhab.ui.habot.nlp.Intent;
import org.openhab.ui.habot.nlp.IntentInterpretation;
import org.openhab.ui.habot.nlp.Skill;
import org.openhab.ui.habot.nlp.UnsupportedLanguageException;
import org.openhab.ui.habot.nlp.internal.IntentTrainer;

public class TrainerTest {

    IntentTrainer trainer = null;

    List<Skill> skills;

    public class MockSkill implements Skill {

        private String intent;

        public MockSkill(String intent) {
            this.intent = intent;
        }

        @Override
        public String getIntentId() {
            return intent;
        }

        @Override
        public InputStream getTrainingData(String language) throws UnsupportedLanguageException {
            return MockSkill.class.getClassLoader().getResourceAsStream("train/" + language + "/" + intent + ".txt");
        }

        @Override
        public IntentInterpretation interpret(Intent intent, String language) {
            // TODO Auto-generated method stub
            return null;
        }
    }

    // @Before
    protected void initializeMockSkills() {
        skills = new ArrayList<Skill>();

        skills.add(new MockSkill("get-status"));
        skills.add(new MockSkill("activate-object"));
        skills.add(new MockSkill("deactivate-object"));
        skills.add(new MockSkill("get-history-daily"));
        skills.add(new MockSkill("get-history-hourly"));
        skills.add(new MockSkill("get-history-weekly"));
        skills.add(new MockSkill("get-history-monthly"));
        skills.add(new MockSkill("get-history-last-changes"));
    }

    protected Intent interpret(String query) {
        System.out.println("----");
        System.out.println("\"" + query + "\"");
        System.out.println(new TreeMap<>(this.trainer.getScoreMap(query)).descendingMap().toString());
        Intent intent = this.trainer.interpret(query);
        System.out.println(intent.toString());
        return intent;
    }

    @Test
    public void testEN() throws Exception {
        initializeMockSkills();
        this.trainer = new IntentTrainer("en", skills);

        interpret("Temperature in the bedroom?");
        interpret("show me the temperature in the kitchen");
        interpret("what's the temperature in the garage?");

        interpret("start the heating in the garage");
        interpret("start the ac please!");
        interpret("turn on the lights in the kitchen");
        interpret("please switch the lights in the kitchen on");
        interpret("please turn off the radiators");
        interpret("deactivate the alarm");
        interpret("stop the music in the living room");

        interpret("show me a graph of the temperature in the living room for the last 3 hours");
        interpret("graph the water consumption for the last 2 days");
        interpret("i'd like a chart of the humidity over 2 weeks");
        interpret("temperature in the downstairs corridor for the last month");
        interpret("luminosity in the living room over 6 months");

        interpret("when was the alarm turned on for the last time?");
        interpret("show me the last state changes of the window in the bedroom");
    }

    @Test
    public void testFR() throws Exception {
        this.trainer = new IntentTrainer("fr", skills);

        interpret("donne-moi un graphique de la température du salon pour les dernières heures");
        interpret("montre-moi la température du salon");
        interpret("Température du salon ?");
        interpret("active le thermostat dans le garage");
        interpret("mets en route la climatisation");
        interpret("montre le graphique de la consommation électrique pour les 2 derniers jours");
    }

    @Test
    public void testDE() throws Exception {
    	initializeMockSkills();
        this.trainer = new IntentTrainer("de", skills);

        interpret("mach den Fernseher an");

    }    
}
