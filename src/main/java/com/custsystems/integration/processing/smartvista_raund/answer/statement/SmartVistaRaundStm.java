package com.custsystems.integration.processing.smartvista_raund.answer.statement;

import com.custsystems.custerp.kernel.custerpProperties.IPropertiesReader;
import com.custsystems.custtools.XmlTools;
import com.custsystems.ibankfl.tools.currency.CurrencyEmpty;
import com.custsystems.ibankfl.tools.currency.ICurrencyInfo;
import com.custsystems.integration.card.ProcessingControlException;
import com.custsystems.integration.card.ProcessingRequestException;
import com.custsystems.integration.card.custom.answer.ICardStatement;
import com.custsystems.integration.card.custom.answer.ICardStatementRecord;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.math.BigDecimal;
import java.util.*;

public class SmartVistaRaundStm implements ICardStatement {
    protected Collection<ICardStatementRecord> transactions;
    protected BigDecimal closingBalance;
    protected BigDecimal creditLimit;
    protected BigDecimal currentBalance;
    protected BigDecimal openingBalance;
    protected BigDecimal totalBlocked;
    protected boolean main;
    protected ICurrencyInfo currency;
    protected String cardExpire;
    protected IPropertiesReader properties;

    public SmartVistaRaundStm(Object data, IPropertiesReader properties) throws ProcessingRequestException, ProcessingControlException {
        this.properties = properties;
        this.init(data);
    }

    @Override
    public BigDecimal getClosingBalance() {
        return this.closingBalance;
    }

    public void setClosingBalance(BigDecimal closingBalance) {
        this.closingBalance = closingBalance;
    }

    @Override
    public BigDecimal getCreditLimit() {
        return this.creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    @Override
    public BigDecimal getCurrentBalance() {
        return this.currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    @Override
    public BigDecimal getTotalBlocked() {
        return this.totalBlocked;
    }

    public void setTotalBlocked(BigDecimal totalBlocked) {
        this.totalBlocked = totalBlocked;
    }

    @Override
    public BigDecimal getOpeningBalance() {
        return this.openingBalance;
    }

    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }

    @Override
    public boolean isMain() {
        return this.main;
    }

    public void setMain(boolean main) {
        this.main = main;
    }

    @Override
    public Collection<ICardStatementRecord> getTransactions() {
        return (Collection) (this.transactions != null ? this.transactions : Collections.emptyList());
    }

    public void setTransactions(Collection<ICardStatementRecord> transactions) {
        this.transactions = transactions;
    }

    @Override
    public ICurrencyInfo getCurrency() {
        return (ICurrencyInfo) (this.currency != null ? this.currency : CurrencyEmpty.instance);
    }

    public void setCurrency(ICurrencyInfo currency) {
        this.currency = currency;
    }

    @Override
    public String getCardExpire() {
        return this.cardExpire;
    }

    public void setCardExpire(String cardExpire) {
        this.cardExpire = cardExpire;
    }

    @Override
    public Iterator<ICardStatementRecord> iterator() {
        return this.getTransactions().iterator();
    }

    public void init(Object data) throws ProcessingRequestException, ProcessingControlException {
        if (!(data instanceof Document)) {
            throw new ProcessingRequestException("SmartVista Raund Api-Gate response object not instance of XML document!");
        } else {
            Document doc = (Document) data;
            Element e1 = doc.getDocumentElement();
            Element e2 = XmlTools.getBranchElement(e1, "Body/getTransactionsResponse");
            Element e3 = XmlTools.getBranchElement(e2, "transactions/transaction");
            this.setTransactions(new ArrayList());

            //Типы операций, которые включаем в выписку.
            List typeOper = Lists.newArrayList(Splitter.on(",").split(this.properties.getString("smartvista_raund/stm_codes", "659,677,680,760,757,774,700,702,781,785")));
            while (e3 != null) {
                if (typeOper.contains(XmlTools.getStringEx(e3, "transactionType", (String) null)))
                    if (BigDecimal.valueOf(XmlTools.getLongEx(e3, "amount").longValue(), 2).compareTo(BigDecimal.ZERO) != 0) { // больше нуля
                        this.getTransactions().add(new SmartVistaRaundStmRecord(e3));
                    }
                e3 = XmlTools.getNextElement(e3, "transaction");
            }
        }


    }
}
