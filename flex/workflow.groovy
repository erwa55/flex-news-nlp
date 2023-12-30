package news

import com.ooyala.flex.enterprise.client.service.AssetApiService
import com.ooyala.flex.enterprise.client.service.TaxonomyApiService
import com.ooyala.flex.plugins.GroovyScriptContext
import com.ooyala.flex.query.asset.AssetApiQuery
import com.ooyala.flex.sdk.FlexSdkClient
import com.ooyala.flex.model.userdefinedobject.create.NewUserDefinedObject
import com.ooyala.flex.taxonomy.model.Taxon
import com.ooyala.flex.taxonomy.model.Taxonomy
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.xml.XmlSlurper

import java.text.SimpleDateFormat

class rssImport {

    GroovyScriptContext context;
    FlexSdkClient flexSdkClient;

    private final TaxonomyApiService taxonomyApiService

    def execute() {

        // Set RSS feed to read
        def rssFeedUrl = 'https://rss.nytimes.com/services/xml/rss/nyt/World.xml' // Replace with your RSS feed URL

        // Set the locale to English and the timezone to UTC for date parsing
        def dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
        def currentTime = new Date()


        // Parsing
        def parser = new XmlSlurper()
        def rss = parser.parse(rssFeedUrl)

        rss.channel.item.each { item ->


            // transform date of the article
            def pubDate = item.pubDate.text()
            def date = dateFormat.parse(pubDate)
            // Set the time it was 30 before actual time
            def thirtyMinutesAgo = new Date(currentTime.time - 30 * 60 * 1000)
            context.logInfo("$date  vs $thirtyMinutesAgo ")


            if (date.before(thirtyMinutesAgo)) {

                context.logInfo("Title: ${item.title.text()}\n" +
                        "Description: ${item.description.text()}\n" +
                        "Categorie: ${item.category.text()}\n" +
                        "Publication Date: ${item.pubDate.text()}\n" +
                        "Link: ${item.guid.text()}")
                context.logInfo("-----------------------------")

                udoBuilder(item.title.text() as String, item.description.text() as String, item.guid.text() as String, item.category.text() as String)
            }
        }

        context.logWarn("All good Sir =')")


        context.logInfo(" \n" +
                "▄───▄\n" +
                "█▀█▀█\n" +
                "█▄█▄█\n" +
                "─███──▄▄\n" +
                "─████▐█─█\n" +
                "─████───█\n" +
                "─▀▀▀▀▀▀▀\n")

    }


    def udoBuilder(String name, String description, String link, String category) {

        // prepare udo build
        def wire = NewUserDefinedObject.builder()
                .name(name)
                .workspaceId(509529)
                .build()

        //build UDO
        def udoService = flexSdkClient.userDefinedObjectService;
        def createdUDO = udoService.createUserDefinedObject('wires', wire);


        //Retrieve UDO and modify metadata
        def field = udoService.getObjectData('wires', createdUDO.id);


        //Set the metadata to UDO
        field.getField('description').setValue(description);
        context.logWarn(" Cat is : $category")
        field.getField('theme').setValue(classify(description))

        field.getField('url').setValue(link);


        udoService.setObjectData('wires', createdUDO.id, field)


        context.logInfo("New wire created =D")
        return createdUDO.id


    }


    def classify(String description) {

        def apiUrl = 'https://b2b2-51-210-82-58.ngrok-free.app/classify'
        //TODO get candidates label by listing taxonomies

        def requestContent = [
                sequence_to_classify: "$description",
                candidate_labels    : ["Art", "Economy", "Health", "Politic", "Science", "Society", "Sports", "Traffic", "Weather", "World"]
        ]

        def url = new URL(apiUrl)
        def connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = 'POST'
        connection.doOutput = true
        connection.setRequestProperty('Content-Type', 'application/json')
        connection.outputStream.withWriter('UTF-8') { writer ->
            writer.write(new JsonBuilder(requestContent).toString())
        }

        def response = connection.inputStream.withReader('UTF-8') { reader ->
            new JsonSlurper().parse(reader)
        }

        context.logInfo("Status: ${connection.responseCode}" + " Highest Score Label: ${response.label}")

        connection.disconnect()

        return response.label as String
    }
    

}
