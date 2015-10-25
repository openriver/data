
@Grab('com.xlson.groovycsv:groovycsv:1.0')
import static com.xlson.groovycsv.CsvParser.parseCsv

def QUARTER = "Quarter"
def TOWN_HALL = "Town hall"
def CIVIC_CENTRE = "Civic centre"
def REC_FIELD = "Rec field"
def RINK = "Rink"

def QUARTERS = ["Q1", "Q2", "Q3", "Q4"]
def BUILDINGS = [TOWN_HALL, CIVIC_CENTRE, REC_FIELD, RINK]

class IncomingFields {
    def quarter
    def meterId
    def building
    def usage
}

class OutgoingFields {
    def quarter
    def usages = []
}

def parseLine = { def line ->
    def fields = new IncomingFields()

    fields.quarter = line.getAt 0
    fields.meterId = line.getAt 1
    fields.building = line.getAt 2
    fields.usage = line.getAt 3 

    return fields
}

def parseFile = { def file ->
    def rows = [] 
    def text = new File(file).getText()
    def data = parseCsv text

    data.each { def line ->
        def fields = parseLine(line)
        rows << fields
    }

    return rows
}

def transform = { def inFields ->
    def outFields = [] 

    QUARTERS.each { def quarter ->
        def outField = new OutgoingFields()
        outField.quarter = quarter
        def usages = []
        BUILDINGS.each { def building ->
            usages << inFields.find { (it.quarter == quarter) && (it.building == building) }.usage
        }
        outField.usages = usages
        outFields << outField
    }

    return outFields
}

def buildData = { def outFields ->
    def rows = ""

    outFields.each { def out ->
        def row = "["
        row += "'${out.quarter}',"
        def lastIndex = 3
        out.usages.eachWithIndex { def usage, def index ->
             row += "${usage}"
             if (index != lastIndex) { row += "," }
        }
        row += "],\n"
        rows += row
    }

    return rows
}

// ------- MAIN 

def incomingRows = parseFile("../../2015-TOWN-ELECTRIC.csv")

def outgoingRows = transform(incomingRows)

def data = buildData(outgoingRows) 

def html = """
<html>
  <head>
    <script type="text/javascript" src="https://www.google.com/jsapi"></script>
    <script type="text/javascript">
      google.load("visualization", "1", {packages:["corechart"]});
      google.setOnLoadCallback(drawVisualization);

      function drawVisualization() {
        // Some raw data (not necessarily accurate)
        var data = google.visualization.arrayToDataTable([
['${QUARTER}', '${TOWN_HALL}', '${CIVIC_CENTRE}', '${REC_FIELD}', '${RINK}'],
${data}
      ]);

    var options = {
      title : 'Electric Usage by Open River Municipal Properties',
      vAxis: {title: 'Usage'},
      hAxis: {title: 'Quarter'},
      seriesType: 'bars',
      series: {5: {type: 'line'}}
    };

    var chart = new google.visualization.ComboChart(document.getElementById('chart_div'));
    chart.draw(data, options);
  }
    </script>
  </head>
  <body>
    <div id="chart_div" style="width: 900px; height: 700px;"></div>
  </body>
</html>
"""

println html
