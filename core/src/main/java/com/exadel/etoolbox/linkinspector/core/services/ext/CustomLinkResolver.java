package com.exadel.etoolbox.linkinspector.core.services.ext;

import com.exadel.etoolbox.linkinspector.api.service.LinkTypeProvider;
import com.exadel.etoolbox.linkinspector.core.models.Link;

import java.util.List;
import java.util.stream.Stream;

public interface CustomLinkResolver {
    List<Link> getLinks(String text);
}
