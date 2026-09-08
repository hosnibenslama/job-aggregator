package com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.reader;

import com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.Advantage;
import com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.Article;
import com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.ExternalId;
import com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.MarketedObject;
import com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.Role;
import com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.Tarif;
import com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.feed.ContractFeedMapper;
import com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.domain.feed.FeedRecord;
import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates child records for a single marketed object (OM) during assembly.
 * Package-private — used exclusively by {@link ContractBlockAssembler}.
 */
final class MarketedObjectBuilder {

    final FeedRecord record;
    final List<ExternalId> externalIds = new ArrayList<>();
    final List<Role> roles = new ArrayList<>();
    final List<Tarif> tarifs = new ArrayList<>();
    final List<Advantage> advantages = new ArrayList<>();
    final List<ArticleBuilder> articleBuilders = new ArrayList<>();

    MarketedObjectBuilder(FeedRecord record) {
        this.record = record;
    }

    MarketedObject build() {
        MarketedObject base = ContractFeedMapper.toMarketedObject(record);
        List<Article> articles = articleBuilders.stream()
                .map(ArticleBuilder::build)
                .toList();

        return new MarketedObject(
                base.omId(),
                base.businessRelationship(),
                List.copyOf(externalIds),
                List.copyOf(roles),
                List.copyOf(tarifs),
                List.copyOf(advantages),
                List.copyOf(articles)
        );
    }
}
