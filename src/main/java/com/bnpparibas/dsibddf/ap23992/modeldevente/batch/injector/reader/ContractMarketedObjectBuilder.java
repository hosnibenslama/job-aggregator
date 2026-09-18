package com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.reader;

import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Advantage;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Article;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.ExternalId;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.MarketedObject;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Role;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.Tarif;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.feed.ContractFeedMapper;
import com.bnpparibas.dsibddf.ap23992.modeldevente.batch.injector.domain.feed.FeedRecord;
import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates child records for a single marketed object (OM) during assembly.
 * Package-private — used exclusively by {@link ContractBlockAssembler}.
 */
final class ContractMarketedObjectBuilder {

    final FeedRecord record;
    final List<ExternalId> externalIds = new ArrayList<>();
    final List<Role> roles = new ArrayList<>();
    final List<Tarif> tarifs = new ArrayList<>();
    final List<Advantage> advantages = new ArrayList<>();
    final List<ContractArticleBuilder> articleBuilders = new ArrayList<>();

    ContractMarketedObjectBuilder(FeedRecord record) {
        this.record = record;
    }

    MarketedObject build() {
        MarketedObject base = ContractFeedMapper.toMarketedObject(record);
        List<Article> articles = articleBuilders.stream()
                .map(ContractArticleBuilder::build)
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
