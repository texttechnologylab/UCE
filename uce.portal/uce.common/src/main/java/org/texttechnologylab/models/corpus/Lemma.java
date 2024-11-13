package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;
import org.texttechnologylab.models.WikiModel;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="lemma")
public class Lemma extends UIMAAnnotation implements WikiModel {
    @OneToOne()
    @JoinColumn(name="document_id")
    private Document document;

    private String value;

    /* POS properties */
    private String posValue;
    private String coarseValue;
    /* POS properties */

    /* Morph properties */
    private String animacy;
    private String aspect;
    private String casee;
    private String definiteness;
    private String degree;
    private String gender;
    private String mood;
    private String negative;
    private String number;
    private String numberType;
    private String person;
    private String possessive;
    private String pronType;
    private String reflex;
    private String tense;
    private String verbForm;
    private String voice;
    /* Morph properties */

    public Lemma(int begin, int end) {
        super(begin, end);
    }
    public Lemma() { super(); }

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
}
