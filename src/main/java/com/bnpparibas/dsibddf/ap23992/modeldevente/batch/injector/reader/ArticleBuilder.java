package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.reader;

import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Account;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Advantage;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Article;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Condition;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.ExternalId;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Ikac;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Role;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Tarif;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.feed.ContractFeedMapper;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.feed.FeedRecord;
import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates child records for a single article (ART) during assembly.
 * Package-private — used exclusively by {@link ContractBlockAssembler}.
 */
final class ArticleBuilder {

    final FeedRecord record;
    final List<ExternalId> externalIds = new ArrayList<>();
    final List<Ikac> ikacs = new ArrayList<>();
    final List<Condition> conditions = new ArrayList<>();
    final List<Account> accounts = new ArrayList<>();
    final List<Role> roles = new ArrayList<>();
    final List<Tarif> tarifs = new ArrayList<>();
    final List<Advantage> advantages = new ArrayList<>();

    ArticleBuilder(FeedRecord record) {
        this.record = record;
    }

    Article build() {
        Article base = ContractFeedMapper.toArticle(record);
        return new Article(
                base.sequentialIndex(),
                List.copyOf(externalIds),
                List.copyOf(ikacs),
                List.copyOf(conditions),
                List.copyOf(accounts),
                List.copyOf(roles),
                List.copyOf(tarifs),
                List.copyOf(advantages)
        );
    }
}
