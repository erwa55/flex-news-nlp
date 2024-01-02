package news


import com.ooyala.flex.plugins.GroovyScriptContext
import com.ooyala.flex.sdk.FlexSdkClient
import com.ooyala.flex.model.userdefinedobject.create.NewUserDefinedObject
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.xml.XmlSlurper

import java.text.SimpleDateFormat
import java.util.regex.Matcher

class rssImport {

    GroovyScriptContext context;
    FlexSdkClient flexSdkClient;

    //class variables for AI services
    List<String> taxonNames = []


    // START
    def execute() {

        // Set RSS feed to read
        def rssFeedUrl = 'https://rss.nytimes.com/services/xml/rss/nyt/World.xml' // Replace with your RSS feed URL

        // Set the locale to English and the timezone to UTC for date parsing
        def dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
        def currentTime = new Date()



        // Get taxons of the first level and populate class variables for AI service
        def taxonData = flexSdkClient.taxonomyService.getRootTaxons(812205222) as String
        taxonData.tokenize('),').each {
            String taxon = it.trim()
            // Match the name field in the string
            Matcher m = taxon =~ /name=([^,]+)/
            if (m.find()) {
                String name = m.group(1)
                taxonNames.add(name)
            }
        }
        context.logInfo("All taxon names: $taxonNames")


        // Parsing RSS
        def parser = new XmlSlurper()
        def rss = parser.parse(rssFeedUrl)

        rss.channel.item.each { item ->

            /*
            In this parsing, I implement a complete logic to import only those articles that have been published in the last 30 minutes.
            This method allows me to safely import new articles every 30 minutes and ensure that they are not imported twice.
            This method is of course not perfect, but it's quick to implement and doesn't require any database calls, so it's much more efficient.

             ATTENTION: If this were to be implemented in production, I do not recommend using it ! Instead suggest exploring methods based
             on the comparison of publication dates plus tokens generated in the day (e.g. if the date corresponds to today and the checksum
             of the url does not exist in this week's list. Then matter )

             */
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

                // Create the UDO
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

    // END
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
        field.getField('url').setValue(link);

        //Call NLP service to get theme base on AI analysis
        field.getField('theme').setValue(classify(description))


        //  Save metadata
        udoService.setObjectData('wires', createdUDO.id, field)


        context.logInfo("New wire created =D")
        return createdUDO.id


    }


    def classify(String description) {

        // Set service URL -  Change by your own service address
        def apiUrl = 'https://app.app/classify'

        //Build  request body
        def requestContent = [
                sequence_to_classify: "$description",
                candidate_labels    : taxonNames
        ]

        //Call AI service
        def url = new URL(apiUrl)

        def connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = 'POST'
        connection.doOutput = true
        connection.setRequestProperty('Content-Type', 'application/json')
        connection.outputStream.withWriter('UTF-8') { writer ->
            writer.write(new JsonBuilder(requestContent).toString())
        }

        //Parse AI reponse
        def response = connection.inputStream.withReader('UTF-8') { reader ->
            new JsonSlurper().parse(reader)
        }

        context.logInfo("Status: ${connection.responseCode}" + " Highest Score Label: ${response.label}")

        connection.disconnect()

        return response.label as String
    }


}
