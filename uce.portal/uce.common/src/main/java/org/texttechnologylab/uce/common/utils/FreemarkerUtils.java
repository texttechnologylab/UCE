package org.texttechnologylab.uce.common.utils;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateSequenceModel;

import java.util.ArrayList;
import java.util.List;


public class FreemarkerUtils {
    public static List<ArrayList<Integer>> convertToNestedIntegerList(TemplateSequenceModel outerSeq)
            throws TemplateModelException {

        List<ArrayList<Integer>> result = new ArrayList<>();

        for (int i = 0; i < outerSeq.size(); i++) {
            TemplateModel innerModel = outerSeq.get(i);

            if (innerModel instanceof TemplateSequenceModel) {
                TemplateSequenceModel innerSeq = (TemplateSequenceModel) innerModel;
                ArrayList<Integer> innerList = new ArrayList<>();

                for (int j = 0; j < innerSeq.size(); j++) {
                    TemplateModel valueModel = innerSeq.get(j);
                    if (valueModel instanceof TemplateNumberModel) {
                        int val = ((TemplateNumberModel) valueModel).getAsNumber().intValue();
                        innerList.add(val);
                    } else {
                        throw new TemplateModelException("Expected number at [" + i + "][" + j + "]");
                    }
                }

                result.add(innerList);
            } else {
                throw new TemplateModelException("Expected sequence at index " + i);
            }
        }

        return result;
    }
}
