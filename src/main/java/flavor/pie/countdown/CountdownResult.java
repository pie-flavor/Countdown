package flavor.pie.countdown;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.economy.transaction.TransactionType;
import org.spongepowered.api.service.economy.transaction.TransferResult;

import java.math.BigDecimal;
import java.util.Set;

public class CountdownResult {
    private CountdownResult() {}
    public static class Transaction implements TransactionResult {
        Account account;
        Currency currency;
        BigDecimal amount;
        Set<Context> contexts;
        ResultType result;
        TransactionType type;

        public Transaction(Account account, Currency currency, BigDecimal amount, Set<Context> contexts, ResultType result, TransactionType type) {
            this.account = account;
            this.currency = currency;
            this.amount = amount;
            this.contexts = contexts;
            this.result = result;
            this.type = type;
        }

        @Override
        public Account getAccount() {
            return account;
        }

        @Override
        public Currency getCurrency() {
            return currency;
        }

        @Override
        public BigDecimal getAmount() {
            return amount;
        }

        @Override
        public Set<Context> getContexts() {
            return contexts;
        }

        @Override
        public ResultType getResult() {
            return result;
        }

        @Override
        public TransactionType getType() {
            return type;
        }
    }

    public static class Transfer extends Transaction implements TransferResult {

        Account accountTo;

        public Transfer(Account account, Account accountTo, Currency currency, BigDecimal amount, Set<Context> contexts, ResultType result, TransactionType type) {
            super(account, currency, amount, contexts, result, type);
            this.accountTo = accountTo;
        }

        @Override
        public Account getAccountTo() {
            return accountTo;
        }
    }
}
