package org.texttechnologylab;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import io.swagger.models.auth.In;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIRemoteDriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.texttechnologylab.annotation.*;
import org.xml.sax.SAXException;


import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;


public class RunDUUIPipeline {
    public DUUIInformation getModelResources(List<ModelInfo> modelGroups, String inputText) throws Exception {
        HashMap<String, String> urls = new HashMap<>();
        for (ModelInfo modelGroup : modelGroups) {
            String url = modelGroup.getUrl();
            String name = modelGroup.getName();
            urls.put(name, url);
        }
        DUUIPipeline pipeline = new DUUIPipeline();
        // Add language detection
        JCas cas = pipeline.getLanguage(inputText);
        // get cas sentences

        cas = pipeline.getSentences(cas);
        // run pipeline
        DUUIComposer composer = pipeline.setListComposer(urls);
        JCas result = pipeline.runPipeline(cas, composer);
        // get results
        Object[] results = pipeline.getJCasResults(result, modelGroups);
        // print results
        Sentences sentences = (Sentences) results[0];
        TextClass textClass = (TextClass) results[1];
        return new DUUIInformation(sentences, textClass);
    }

    public static void main(String[] args) throws Exception {
        ModelResources modelResources = new ModelResources();
        List<ModelGroup> modelGroups = modelResources.getGroupedModelObjects();
        List<ModelInfo> modelInfos = new ArrayList<>();
        for (ModelGroup modelGroup : modelGroups) {
            List<ModelInfo> models = modelGroup.getModels();
            modelInfos.addAll(models);
        }
        String inputText = "Das ist ein Text, welches über Sport und Fußball handelt. Der Fußball Lionel Messi hat in der 25min. ein Tor gegen Real Madrid geschossen! Dadruch hat Barcelona gewonnen.";
        DUUIInformation duuiInformation = new RunDUUIPipeline().getModelResources(modelInfos, inputText);

    }

}
