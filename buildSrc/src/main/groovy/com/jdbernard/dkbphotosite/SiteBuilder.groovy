package com.jdbernard.dkbphotosite

import groovy.text.SimpleTemplateEngine
import groovy.text.Template

public class SiteBuilder {

    private final SimpleTemplateEngine engine = new SimpleTemplateEngine()
    private final Template template
    private final def siteConfig

    public SiteBuilder(Reader templateSource, def siteConfig) {
        this.template = engine.createTemplate(templateSource)
        this.siteConfig = siteConfig
    }

    public String generatePage(Category category) {
        def binding = [ siteConfig: this.siteConfig,
                        category: category]
        return template.make(binding)
    }

    public void buildPages(File outputDir, Category category) {
        if (!outputDir.exists()) outputDir.mkdirs()
        File outputFile = new File(outputDir, 'index.html')
        outputFile.text = generatePage(category)

        category.subcategories.each { buildPages(new File(outputDir, it.name), it) }
    }
}
