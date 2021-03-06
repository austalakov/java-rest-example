package com.cagataygurturk.services.transaction;


import com.cagataygurturk.models.Sum;
import com.cagataygurturk.models.Transaction;
import com.cagataygurturk.services.transaction.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

@Component
public class TransactionServiceImpl implements TransactionService {

    protected TransactionRepository repository;
	
	private static final Double FACTOR = 1.1;
	private static final Double MIN_AMOUNT = 10.0;

    @Autowired
    public TransactionServiceImpl(@Qualifier("transaction_repository_inmemory")
                                  TransactionRepository repository) {

        this.repository = repository;

    }

    public Transaction getTransactionById(long transactionId) throws TransactionNotFoundException {

        Transaction transaction = this.repository.getTransactionById(transactionId);

        if (null == transaction) {
            throw new TransactionNotFoundException("Transaction with id " + transactionId + " not found");
        }

        return transaction;
    }

    private void checkTransactionValid(Transaction transaction) {
        if(transaction.getAmount() < MIN_AMOUNT) {
            throw new InvalidTransactionException("Transaction is invalid: amount needs to be greater than " + MIN_AMOUNT.toString());
        }
        if(transaction.getType().isEmpty()) {
            throw new InvalidTransactionException("Transaction is invalid: type must be specified");
        }
    }

    protected Transaction saveTransactionToRepository(Transaction transaction) {
        checkTransactionValid(transaction);
        return this.repository.saveTransaction(transaction);
    }

    public Transaction createNewTransaction(double amount, String type) {
        Transaction transaction = new Transaction(amount, type);
        return this.saveTransactionToRepository(transaction);
    }

    public Transaction createNewTransaction(double amount, String type, long parentId) throws TransactionNotFoundException {
        /**
         * Get parent transaction
         */
        Transaction parentTransaction = this.getTransactionById(parentId);
        Transaction transaction = new Transaction(amount, type, parentTransaction);
        return this.saveTransactionToRepository(transaction);
    }
	
	public Transaction updateTransaction(long transactionId, double amount, String type) throws TransactionNotFoundException {
        /**
         * Get transaction
         */
        Transaction transaction = this.getTransactionById(transactionId);
        transaction.setAmount(amount);
		transaction.setType(type);
		System.out.println(calculateSum(transaction)); // for logging
        return this.saveTransactionToRepository(transaction);
    }

    public Sum calculateSum(Transaction transaction) {
        double sum = 0;

        Map<Long, Transaction> children = repository.getTransactionByCriteria("parentId", transaction.getId());
        Iterator it = children.values().iterator();

        while (it.hasNext()) {
            sum += ((Transaction) it.next()).getAmount();
        }
		
		sum *= FACTOR;

        return new Sum(sum);
    }

    public ArrayList<Long> getTransactionsByType(String type) {

        ArrayList<Long> transactions = new ArrayList<>();
        Map<Long, Transaction> children = repository.getTransactionByCriteria("type", type);

        children.forEach((key, object) -> {
            transactions.add(object.getId());
        });

        return transactions;
    }

}
