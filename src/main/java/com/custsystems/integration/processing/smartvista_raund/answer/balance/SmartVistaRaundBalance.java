package com.custsystems.integration.processing.smartvista_raund.answer.balance;

import com.custsystems.custtools.XmlTools;
import com.custsystems.integration.card.ProcessingControlException;
import com.custsystems.integration.card.ProcessingRequestException;
import com.custsystems.integration.card.custom.answer.CardBalance;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.math.BigDecimal;

public class SmartVistaRaundBalance extends CardBalance {
    public SmartVistaRaundBalance(Object data) throws ProcessingRequestException, ProcessingControlException {
        super(data);
    }

    public static BigDecimal fixResponseAmount(Element e, String name) {
        return BigDecimal.valueOf(XmlTools.getLongEx(e, name).longValue(), 2);
    }

    @Override
    public void init(Object data) throws ProcessingRequestException, ProcessingControlException {
        if (!(data instanceof Document)) {
            throw new ProcessingRequestException("SmartVista Raund Api-Gate response object not instance of XML document!");
        } else {
            Document document = (Document) data;
            Element e1 = document.getDocumentElement();
            //Element faultResponse = XmlTools.getBranchElement(e1, "ServiceLevelFault");
            Element cardBalanceResponse = XmlTools.getBranchElement(e1, "Body/getCardBalanceResponse");
         /*String code = XmlTools.getStringEx(faultResponse, "errorCode");
         if (faultResponse!=null) {
            ProcessingConnectorSmartVistaRaund.getLogger().trace("Trace faultResponse:" + faultResponse.toString());
            ProcessingConnectorSmartVistaRaund.getLogger().trace("Trace code:" + code.toString() );
         }
         if(!code.isEmpty()) {
            ProcessingConnectorSmartVistaRaund.getLogger().error("Error #" + code + ": " + XmlTools.getStringEx(faultResponse, "errorDesc"));
            throw new ProcessingRequestException(code + ": " + XmlTools.getStringEx(faultResponse, "errorDesc"));
         } else {*/
            this.setAmountAvailable(fixResponseAmount(cardBalanceResponse, "balance"));  //Текущий баланс
            this.setCreditLimit(fixResponseAmount(cardBalanceResponse, "availableExceedLimit")); //Кредитный лимит
            //}
        }
    }
}
