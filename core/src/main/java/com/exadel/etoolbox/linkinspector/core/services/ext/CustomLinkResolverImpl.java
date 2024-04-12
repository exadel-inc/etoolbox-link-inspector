package com.exadel.etoolbox.linkinspector.core.services.ext;

import com.exadel.etoolbox.linkinspector.api.service.LinkTypeProvider;
import com.exadel.etoolbox.linkinspector.core.models.Link;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(service = CustomLinkResolver.class)
public class CustomLinkResolverImpl implements CustomLinkResolver{
    private List<LinkTypeProvider> providers = new ArrayList<>();

    @Override
    public List<LinkTypeProvider> getLinkTypeProviders() {
        return providers;
    }

    @Override
    public List<Link> getLinks(String propertyValue){
        return providers.stream().filter(p->p.getLinkValue(propertyValue)!=null)
                .map(p->new Link(p, p.getLinkValue(propertyValue))).collect(Collectors.toList());
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addLinkValidator(LinkTypeProvider linkTypeProvider, Map<String, Object> properties) {
        providers.add(linkTypeProvider);
    }

    public void removeLinkValidator(Map<String,Object> props) {

    }
}
