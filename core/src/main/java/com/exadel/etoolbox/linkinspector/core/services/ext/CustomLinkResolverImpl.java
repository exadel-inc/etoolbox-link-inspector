package com.exadel.etoolbox.linkinspector.core.services.ext;

import com.exadel.etoolbox.linkinspector.api.service.LinkTypeProvider;
import com.exadel.etoolbox.linkinspector.core.models.Link;
import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component(service = CustomLinkResolver.class)
public class CustomLinkResolverImpl implements CustomLinkResolver{
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private volatile List<LinkTypeProvider> providers = new ArrayList<>();

    @Override
    public List<Link> getLinks(String propertyValue){
        return providers.stream().map(p->this.getLink(p, propertyValue))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    private Link getLink(LinkTypeProvider linkTypeProvider, String propertyValue){
        String linkValue = linkTypeProvider.getLinkValue(propertyValue);
        return StringUtils.isNotBlank(linkValue)
                ? new Link(linkTypeProvider, linkValue)
                : null;
    }
}
