package org.texttechnologylab.uce.common.models.corpus;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import org.texttechnologylab.uce.common.annotations.Presentation;
import org.texttechnologylab.uce.common.annotations.Typesystem;
import org.texttechnologylab.uce.common.models.UIMAAnnotation;
import org.texttechnologylab.uce.common.models.WikiModel;
import org.texttechnologylab.uce.common.utils.Pair;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lemma")
@Typesystem(types = {
        de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma.class, POS.class, MorphologicalFeatures.class
})
public class Lemma extends UIMAAnnotation implements WikiModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Presentation(name = "Value")
    private String value;

    /* POS properties */
    @Presentation(name = "PoS Tag")
    private String posValue;

    @Presentation(name = "Coarse Value")
    private String coarseValue;

    /* Morph properties */

    @Presentation(name = "Animacy")
    private String animacy;

    @Presentation(name = "Aspect")
    private String aspect;

    @Presentation(name = "Case")
    private String casee;

    @Presentation(name = "Definiteness")
    private String definiteness;

    @Presentation(name = "Degree")
    private String degree;

    @Presentation(name = "Gender")
    private String gender;

    @Presentation(name = "Mood")
    private String mood;

    @Presentation(name = "Negative")
    private String negative;

    @Presentation(name = "Number")
    private String number;

    @Presentation(name = "Number Type")
    private String numberType;

    @Presentation(name = "Person")
    private String person;

    @Presentation(name = "Possessive")
    private String possessive;

    @Presentation(name = "Pronoun Type")
    private String pronType;

    @Presentation(name = "Reflexive")
    private String reflex;

    @Presentation(name = "Tense")
    private String tense;

    @Presentation(name = "Verb Form")
    private String verbForm;

    @Presentation(name = "Voice")
    private String voice;
    /* Morph properties */

    public Lemma(int begin, int end) {
        super(begin, end);
    }

    public Lemma() {
        super();
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    @Override
    public String getWikiId() {
        return "L" + "-" + this.getId();
    }

    public String getAnimacy() {
        return animacy;
    }

    public void setAnimacy(String animacy) {
        this.animacy = animacy;
    }

    public String getAspect() {
        return aspect;
    }

    public void setAspect(String aspect) {
        this.aspect = aspect;
    }

    public String getCasee() {
        return casee;
    }

    public void setCasee(String casee) {
        this.casee = casee;
    }

    public String getDefiniteness() {
        return definiteness;
    }

    public void setDefiniteness(String definiteness) {
        this.definiteness = definiteness;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public String getNegative() {
        return negative;
    }

    public void setNegative(String negative) {
        this.negative = negative;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getNumberType() {
        return numberType;
    }

    public void setNumberType(String numberType) {
        this.numberType = numberType;
    }

    public String getPerson() {
        return person;
    }

    public void setPerson(String person) {
        this.person = person;
    }

    public String getPossessive() {
        return possessive;
    }

    public void setPossessive(String possessive) {
        this.possessive = possessive;
    }

    public String getPronType() {
        return pronType;
    }

    public void setPronType(String pronType) {
        this.pronType = pronType;
    }

    public String getReflex() {
        return reflex;
    }

    public void setReflex(String reflex) {
        this.reflex = reflex;
    }

    public String getTense() {
        return tense;
    }

    public void setTense(String tense) {
        this.tense = tense;
    }

    public String getVerbForm() {
        return verbForm;
    }

    public void setVerbForm(String verbForm) {
        this.verbForm = verbForm;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public String getPosValue() {
        return posValue;
    }

    public void setPosValue(String posValue) {
        this.posValue = posValue;
    }

    public String getCoarseValue() {
        return coarseValue;
    }

    public void setCoarseValue(String coarseValue) {
        this.coarseValue = coarseValue;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Since we print all these properties in the UI, I could add a new
     * div by hand foreach property. Instead, I'm gonna return a list of
     * tuples
     */
    public List<Pair<String, String>> loopThroughProperties() {
        var result = new ArrayList<Pair<String, String>>();
        Class<?> clazz = this.getClass();

        for (var field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Presentation.class)) {
                var annotation = field.getAnnotation(Presentation.class);
                String presentationName = annotation.name();
                try {
                    field.setAccessible(true);
                    Object value = field.get(this);
                    var stringValue = "/";
                    if(value != null) stringValue = value.toString();
                    result.add(new Pair<>(presentationName, stringValue));
                } catch (IllegalAccessException e) {
                    System.out.println("Couldn't access field.");
                }
            }
        }
        return result;
    }
}
