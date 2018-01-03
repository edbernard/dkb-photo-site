<!DOCTYPE html>
<html>
    <head>
        <meta charset=utf-8>
        <title>${site.title}</title>

        <link rel=stylesheet href=/css/microbox.css"/>
        <link rel=stylesheet href=/css/dkb-photo-site.css"/>
    </head>

    <header>
        <h1>David Bernard</h1>
        <nav>
            <ul class=breadcrumbs><li><a href="#">Home</a></li></ul>
            <h3>Categories</h3>
            <ul>
            <#list subcategories as category>
                <li><a href="${category.title}/">${category.title}</a></li>
            </#list>
            </ul>
            <h3>Pages</h3>
            <ul>
                <li><a href="/">Home</a></li>
                <!-- Could be extended to add links to markdown-generated static
                     pages here. -->
            </ul>
        </nav>
        <div class=about>${site.description}</div>
    </header>

    <section>
    <#list subcategories as category>

    </#list>
    </section>
</html>
