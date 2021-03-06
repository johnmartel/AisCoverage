function CoverageUI() {
    var self = this;
    this.loading = false;
    this.sources = [];
    this.selectedSource = null;
    this.changed = true;
    this.minThreshold = 20;
    this.maxThreshold = 50;
    this.savedMinThreshold = -107;
    this.savedMaxThreshold = -101;
    this.thresholdUnit = "%"
    this.savedThresholdUnit = 'dBm';
    this.minExpectedMessages = 100;
    this.exportMultiplicationFactor = 4;
    this.startDate; //is the time when the analysis started (used for sliding window)
    this.endDate;	//Is the end time of the analysis or the point where the analysis is now (used for sliding window)
    this.selectedStartDate;
    this.selectedEndDate;
    this.exportDataType = 'received_messages';
    this.savedExportDataType = 'signal_strength';

    var topleftPoint;//points which define sat box
    var bottomRightPoint
    var shiptrackactive = false;
    var satChartMethod = 'satonly';


    this.setupUI = function () {

        // Set zoom panel positon
        $(".olControlZoom").css('left', zoomPanelPositionLeft);
        $(".olControlZoom").css('top', zoomPanelPositionTop);

        // Set loading panel positon
        var x = $(document).width() / 2 - $("#loadingPanel").width() / 2;
        $("#loadingPanel").css('left', x);

        // Update mouse location when moved
        map.events.register("mousemove", map, function (e) {
            var position = this.events.getMousePosition(e);
            pixel = new OpenLayers.Pixel(position.x, position.y);
            var lonLat = map.getLonLatFromPixel(pixel).transform(
                map.getProjectionObject(), // from Spherical Mercator Projection
                new OpenLayers.Projection("EPSG:4326") // to WGS 1984
            );
            $("#location").html(lonLat.lon.toFixed(4) + ", " + lonLat.lat.toFixed(4));
        });

        // Init expandable panels
        $('#thresholdPanel').expandable({
            header: "Coverage Settings"
        });
        $('#featureDetailsPanel').expandable({
            header: "Source Details"
        });
        $('#sourcesPanel').expandable({
            header: 'Sources </div><div id="selectAllHeader" class="smallText" style="margin:10px;">Select all<input type="checkbox" id="selectall" checked="yes" />'
        });
        $('#exportPanel').expandable({
            header: "Export"
        });
        $('#shiptrackPanel').expandable({
            header: "Ship Tracking"
        });
//    	$('#bottomPanel').expandable({
//    		header: "Satellite Statistics",
//    		maxHeight: "800px"
//    	});
//    	$('#slidingWindowPanel').expandable({
//    		header: "Sliding Window",
//    		maxHeight: "800px"
//    	});

        //add check box listeners
        $(document).on('change', ".sourceCheckbox", function (e) {
            self.sources[this.id].enabled = $(this).is(':checked');
            self.refreshSourceList();
            self.refreshSourceDetails();
            self.changed = true;
        });

        $(document).on('change', "#selectall", function (e) {
            var element = $(this);
            $.each(self.sources, function (key, source) {
                source.enabled = element.is(':checked');
            });
            self.refreshSourceList();
            self.refreshSourceDetails();
            self.changed = true;
        });

        $(document).on('change', "#coverageType", function (e) {
            var maxThresholdToSave = self.maxThreshold;
            var minThresholdToSave = self.minThreshold;
            var thresholdUnitToSave = self.thresholdUnit;
            var exportDataTypeToSave = self.exportDataType;

            self.maxThreshold = self.savedMaxThreshold;
            self.minThreshold = self.savedMinThreshold;
            self.thresholdUnit = self.savedThresholdUnit;
            self.exportDataType = self.savedExportDataType;

            self.savedMaxThreshold = maxThresholdToSave;
            self.savedMinThreshold = minThresholdToSave;
            self.savedThresholdUnit = thresholdUnitToSave;
            self.savedExportDataType = exportDataTypeToSave;

            self.drawSlider();
            self.cleanupCellDetails();

            $('#exportDataType').val(self.exportDataType);

            self.changed = true;
        });

        //Close window listeners
        $('.close').parent().hide();
        $('.close').click(function () {
            $(this).parent().fadeOut(100);
            //remove sat rectangle
//    		boxLayer.removeFeatures([boxLayer.features[0]]);
            boxLayer.removeAllFeatures();

        });

        //Open ship tracking chart listener
        $('#shiptrackingOpenChartBtn').click(function () {
//    		alert("open bar chart");
            self.updateShipTrackBarChart();
            $('#trackingBarchartWindow').fadeIn(100);

        });

        //Draw ship track listener
        $('#shiptrackingMapBtn').click(function () {
            console.log("draw ship track");
//    		self.drawLine(55,6,58,9);
            self.updateShipTrack();

        });

        //Chart method change - listener
        $("#chartMethodSelect").change(function () {
            satChartMethod = $(this).val();
            self.updateBarChart();
        });

        //setup the threshold slider
        self.drawSlider();

        //setup min expected messages per cell slider
        var minExpected = $("#minExpected");
        minExpected.html(self.minExpectedMessages);
        $("#filterSlider").slider({
            min: 0,
            max: 1000,
            value: self.minExpectedMessages,
            slide: function (event, ui) {
                self.minExpectedMessages = ui.value;
                minExpected.html(ui.value);
            },
            change: function (event, ui) {
                self.changed = true;
            }
        });

        //setup sliding window
        self.updateSlidingWindow();

        //setup
        var exportMultiplicationDivHidden = $("#exportMultiHidden");
        var exportMultiplicationDiv = $("#exportMultiplicationFactor");
        exportMultiplicationDivHidden.val(self.exportMultiplicationFactor);
        exportMultiplicationDiv.html(self.exportMultiplicationFactor);
        $("#multiplicationSlider").slider({
            min: 1,
            max: 60,
            value: self.exportMultiplicationFactor,
            slide: function (event, ui) {
                self.minExpectedMessages = ui.value;
                exportMultiplicationDiv.html(ui.value);
                exportMultiplicationDivHidden.val(ui.value);
            }
        });

        //setting the loop function
        var myTimeout;

        function loopFunction() {
            ;
            if (self.changed) {
                self.drawCoverage();
                //self.updateSlidingWindow();
                self.changed = false;
            }
            myTimeout = setTimeout(loopFunction, 2000);
        }

        myTimeout = setTimeout(loopFunction, 2000);

        //map changed listener
        map.events.register('moveend', this, function (event) {

            //update export div
            self.exportMultiplicationFactor = self.getMultiplicationFactor();
            exportMultiplicationDivHidden.val(self.exportMultiplicationFactor);
            exportMultiplicationDiv.html(self.exportMultiplicationFactor);
            $("#multiplicationSlider").slider({value: self.getMultiplicationFactor()});


            //draw coverage
            coverageUI.drawCoverage();
            //self.updateSlidingWindow();

        });
    }

    this.isVDMCategory = function () {
        return $('#coverageType').val() == "VDM";
    }

    this.drawSlider = function () {
        $("#slider-range").dragslider({
            range: true,
            min: self.isVDMCategory() ? 0 : -120,
            max: self.isVDMCategory() ? 100 : -85,
            rangeDrag: true,
            values: [self.minThreshold, self.maxThreshold],
            slide: function (event, ui) {
                self.minThreshold = ui.values[0];
                self.maxThreshold = ui.values[1];
                $("#min-range").html(" < " + self.minThreshold + self.thresholdUnit + " <= ");
                $("#max-range").html(" < " + self.maxThreshold + self.thresholdUnit + " <= ");
            },
            change: function (event, ui) {
                self.changed = true;
            }
        });
        $("#min-range").html(" < " + self.minThreshold + self.thresholdUnit + " <= ");
        $("#max-range").html(" < " + self.maxThreshold + self.thresholdUnit + " <= ");
    }


    this.updateShipTrack = function () {
        aisJsonClient.getShipTrack(selectedStartDate.getTime(), selectedEndDate.getTime(), $('#shiptrackingmmsi').val(), function (trackarray) {
            shiptrackactive = true;
            lineLayer.removeAllFeatures();
            var previousLat = null;
            var previousLon = null;
            var nextRed = false;
            //For each timespan
            $.each(trackarray, function (key, value) {
                //For each lat-lon inside timespan
                $.each(value.positions, function (key1, value1) {
                    if (previousLat != null) {
                        if (nextRed == true) {
                            self.drawLine(previousLat, previousLon, value1.lat, value1.lon, '#CC0000');
                            nextRed = false;
                        } else {
                            self.drawLine(previousLat, previousLon, value1.lat, value1.lon, '#006633');
                        }

                    }
                    previousLat = value1.lat;
                    previousLon = value1.lon;
                });
                nextRed = true;

            });
        });
    }
    this.drawLine = function (lat1, lon1, lat2, lon2, color) {
        var points = new Array(
            new OpenLayers.Geometry.Point(lon1, lat1).transform(new OpenLayers.Projection("EPSG:4326"), map.getProjectionObject()),
            new OpenLayers.Geometry.Point(lon2, lat2).transform(new OpenLayers.Projection("EPSG:4326"), map.getProjectionObject())
        );
        var line = new OpenLayers.Geometry.LineString(points);

        var style = {
            strokeColor: color,
//    	  strokeOpacity: 1.0,
            strokeWidth: 5
        };

        lineFeature = new OpenLayers.Feature.Vector(line, null, style);
        lineLayer.addFeatures([lineFeature]);
    }
    this.updateSlidingWindow = function () {
        aisJsonClient.getStatus(function (data) {
            var startDate = new Date(data.firstMessage);
            startDate.setMinutes(0);
            startDate.setSeconds(0);
            startDate.setMilliseconds(0);
            var endDate = new Date(data.lastMessage + (1000 * 60 * 60));
            endDate.setMinutes(0);
            endDate.setSeconds(0);
            endDate.setMilliseconds(0);

            self.setupSlidingWindow(startDate, endDate);
//    		alert(startDate);
//    		alert(endDate);
//    		alert(data.firstMessage);
        });
    }

    this.setupSlidingWindow = function (startDate, endDate) {
        $('.slidingWindowLabel').hide();
        $("#globalStarTime").html(self.formatDate(startDate));
        $("#globalEndTime").html(self.formatDate(endDate));


        //Update export post form
        $('#exportStartTime').val(startDate.getTime());
        $('#exportEndTime').val(endDate.getTime());

        var timeDif = (endDate.getTime() - startDate.getTime()) / 1000 / 60 / 60;
        var startTimeLabel = $("#starTime");
        var endTimeLabel = $("#endTime");
        var intervalLabel = $("#interval");
//    	var slidingWindowSlider = $( "#slidingWindowOuter" ).width();
        var leftOffset = 37;
        var pixelInterval = $("#slidingWindowOuter").width() / timeDif;
        $("#globalStarTime").css("left", leftOffset);
        $("#globalEndTime").css("left", leftOffset + (pixelInterval * timeDif));


        var updateLabels = function (firstValue, lastValue) {
            selectedStartDate = new Date(startDate.getTime() + firstValue * 1000 * 60 * 60);
            selectedEndDate = new Date(startDate.getTime() + lastValue * 1000 * 60 * 60);
            startTimeLabel.html(self.formatDate(selectedStartDate));
            startTimeLabel.css("left", (firstValue * pixelInterval) + leftOffset);
            endTimeLabel.html(self.formatDate(selectedEndDate));
            endTimeLabel.css("left", (lastValue * pixelInterval) + leftOffset);

            var intervalSize = lastValue - firstValue;
            if (intervalSize > 1) {
                intervalLabel.html(intervalSize + " hours");
            } else {
                intervalLabel.html(intervalSize + " hour");
            }
            intervalLabel.css("left", ((firstValue + (intervalSize / 2)) * pixelInterval) - 25);


        }
        if (typeof defaultFirstValue === "undefined") {
            defaultFirstValue = timeDif - 6;
            defaultSecondValue = timeDif;
            if (timeDif < 6) {
                defaultFirstValue = 0;
            }

        } else {
            //console.log(selectedStartDate);
        }
        updateLabels(defaultFirstValue, defaultSecondValue);


        $("#slidingWindow").dragslider({
            range: true,
            min: 0,
            max: timeDif,
            rangeDrag: true,
            values: [defaultFirstValue, defaultSecondValue],
            slide: function (event, ui) {
                if (ui.values[1] - ui.values[0] < 1) {
                    // do not allow change
                    return false;
                }
                self.canUpdate = true;
                updateLabels(ui.values[0], ui.values[1]);
            },
            change: function (event, ui) {

                //Update export post form
                $('#exportStartTime').val(selectedStartDate.getTime());
                $('#exportEndTime').val(selectedEndDate.getTime());

                //Update sat bar chart if it is visible
                if ($('#barchartpanel').css('display') != 'none') {
                    self.updateBarChart();
                } else if ($('#trackingBarchartWindow').css('display') != 'none') {
                    self.updateShipTrackBarChart();
                }
                if (shiptrackactive == true) {
                    self.updateShipTrack();
                }
                if (self.canUpdate) {
                    self.changed = true;
                    self.canUpdate = false;
                }

            }
        });

        showLabels = false;
        $('#slidingWindowPanel').mouseover(function () {
            if (showLabels == false) {
                $('.slidingWindowLabel').fadeIn(100);
            }
            showLabels = true;

        });
        $('#slidingWindowPanel').mouseout(function () {
            showLabels = false;

            //wait a bit and hide if showlabels is still true.
            setTimeout(function () {
                if (showLabels == false) {
                    $('.slidingWindowLabel').fadeOut(100);
                }
            }, 1000);
        });

        console.log("sliding window updated");
    }

    this.formatDate = function (date) {
        return date.toISOString();
    }


    /**
     * Adds all the layers that will contain graphic.
     */
    this.addLayers = function () {

        // Get renderer
        var renderer = OpenLayers.Util.getParameters(window.location.href).renderer;
        renderer = (renderer) ? [renderer] : OpenLayers.Layer.Vector.prototype.renderers;
        // renderer = ["Canvas", "SVG", "VML"];

        // Add OpenStreetMap Layer
        var osm = new OpenLayers.Layer.OSM(
            "OSM",
            "http://a.tile.openstreetmap.org/${z}/${x}/${y}.png",
            {
                'layers': 'basic',
                'isBaseLayer': true
            }
        );
        map.addLayer(osm);

        //polygon layer
        polygonLayer = new OpenLayers.Layer.Vector("Polygon Layer", {
            styleMap: new OpenLayers.StyleMap({
                "default": new OpenLayers.Style({
                    strokeColor: "black",
                    strokeOpacity: .7,
                    strokeWidth: 1,
                    fillColor: "${fillcolor}",
                    fillOpacity: .6,
                    cursor: "pointer"
                }),
                "select": new OpenLayers.Style({
                    strokeColor: "blue",
                    strokeOpacity: .7,
                    strokeWidth: 4,
                })
            })
        });
//    	polygonLayer.styleMap.styles['default'].defaultStyle={fillColor: 'blue'}
        map.addLayers([polygonLayer]);

        //source layer
        sourceLayer = new OpenLayers.Layer.Vector("Sources", {
            styleMap: new OpenLayers.StyleMap({
                "default": new OpenLayers.Style({
                    strokeColor: "black",
                    strokeOpacity: .7,
                    strokeWidth: 1,
                    fillColor: "${fillcolor}",
                    fillOpacity: .6,
                    cursor: "pointer"
                }),
                "select": new OpenLayers.Style({
                    strokeColor: "blue",
                    strokeOpacity: .7,
                    strokeWidth: 4,
                })
            })
        });
        map.addLayer(sourceLayer);

        //select control for source layer
        selectControl = new OpenLayers.Control.SelectFeature([sourceLayer, polygonLayer],
            {
                clickout: true,
                hover: false,
                highlightOnly: false,
                eventListeners: {
                    featurehighlighted: self.onFeatureSelect,
                    featureunhighlighted: self.onFeatureUnselect
                }

            }
        );
        selectControl.handlers.feature.stopDown = false;
        map.addControl(selectControl);
        selectControl.activate();

        boxLayer = new OpenLayers.Layer.Vector("Box layer");
        map.addLayers([boxLayer]);
//    	var currentBox = null;
        var draw = new OpenLayers.Control.DrawFeature(
            boxLayer,
            OpenLayers.Handler.RegularPolygon,
            {
                featureAdded: function (feature) {
                    if (boxLayer.features.length > 1) {
                        boxLayer.removeFeatures([boxLayer.features[0]]);
////    						console.log("remove");
                    }
//    					currentBox = feature;
                    var g = boxLayer.features[0].clone().geometry; //get geometry of a featyre in your vector layer
                    var vertices = g.getVertices();

                    topleftPoint = vertices[1].transform(map.getProjectionObject(),
                        new OpenLayers.Projection("EPSG:4326"));
                    bottomRightPoint = vertices[3].transform(map.getProjectionObject(),
                        new OpenLayers.Projection("EPSG:4326"));

                    self.updateBarChart();
                    $('#barchartpanel').fadeIn(100);
//    				    console.log("A point has been added");
                },
                handlerOptions: {
                    sides: 4,
                    irregular: true
                }
            }, function () {
                alert('added')
            }
        );
        map.addControl(draw);

        var isDown = false;
        var box;
        $(document).keydown(function (e) {
            if (e.which == 17) {
                if (isDown != true) {
                    isDown = true;
                    draw.activate();
//    				$('.expandable').fadeOut();
                }
                isDown = true;

            }
        });
        $(document).keyup(function (e) {
            if (e.which == 17) {
                isDown = false;
                draw.deactivate();
//    			$('.expandable').fadeIn();
            }
        });

        lineLayer = new OpenLayers.Layer.Vector("Line Layer");
        map.addLayers([lineLayer]);
    }
    this.updateBarChart = function () {
        $('#trackingBarchartWindow').hide();
        $('#barchart').attr('src', 'rest/satExportPNG?random=' + Math.random() + '&startTime=' + selectedStartDate.getTime() + '&endTime=' + selectedEndDate.getTime() + '&lat1=' + topleftPoint.y + '&lon1=' + topleftPoint.x + '&lat2=' + bottomRightPoint.y + '&lon2=' + bottomRightPoint.x + '&satChartMethod=' + satChartMethod);
    }
    this.updateShipTrackBarChart = function () {
        $('#barchartpanel').hide();
        $('#trackingBarchartImg').attr('src', 'rest/shipTrackExportPNG?random=' + Math.random() + '&startTime=' + selectedStartDate.getTime() + '&endTime=' + selectedEndDate.getTime() + '&shipmmsi=' + $('#shiptrackingmmsi').val());
    }
    this.refreshSourceList = function () {
        var sourceContainer = $("#sourcesPanel > .panelContainer");
        sourceContainer.html("");
        var sourceshtml = "";
        $.each(self.sources, function (key, source) {
            var checked = "";
            if (source.enabled) {
                checked = 'checked="checked"';
            }
            sourceshtml += '<div class="legendsText" style="clear: left;height:15px;"><div class="rowElement"><input type="checkbox" class="sourceCheckbox" id="' + source.mmsi + '" ' + checked + ' style="margin-top:0px;" name="" value=""/></div><div class="rowElement" style="width:75px">' + source.name + '</div><div  class="smallText rowElement">' + source.type + '</div></div>';
        });
        sourceContainer.html(sourceshtml);
        self.drawSources();
    }

    this.refreshSourceDetails = function () {

        if (self.selectedSource == null) {
            return;
        }
        var checked = "";
        if (self.selectedSource.enabled) {
            checked = 'checked="checked"';
        }


        $("#featureDetailsPanel > .panelContainer").html('<div class="smallText">Id</div>' +
            '<div class="information">' + feature.mmsi + '</div>' +
            '<div class="smallText">Name</div>' +
            '<div class="information">' + feature.name + '</div>' +
            '<div class="smallText">Type</div>' +
            '<div class="information">' + feature.type + '</div>' +
            '<div class="smallText">Lat</div>' +
            '<div class="information">' + feature.lat.toFixed(4) + '</div>' +
            '<div class="smallText">Lon</div>' +
            '<div class="information">' + feature.lon.toFixed(4) + '</div>' +
            '<div class="smallText">Enabled</div>' +
            '<div class="information"><input type="checkbox" class="sourceCheckbox" id="' + feature.mmsi + '" ' + checked + ' name="" value=""/></div>');
    }

    this.drawSources = function () {
        sourceLayer.removeAllFeatures();
        var drawTheSource = this.drawSource
        $.each(this.sources, function (key, val) {
            drawTheSource(val);
        });
    }

    this.drawSource = function (val) {
        var image;
        if (val.enabled) {
            image = 'img/marker.png';
        } else {
            image = 'img/marker2.png'
        }

        var feature = new OpenLayers.Feature.Vector(
            new OpenLayers.Geometry.Point(val.lon, val.lat).transform(
                new OpenLayers.Projection("EPSG:4326"), // transform from WGS 1984
                map.getProjectionObject() // to Spherical Mercator Projection
            ),
            {hmm: '100'});
//    			 {externalGraphic: image, graphicHeight: 21, graphicWidth: 16, cursor: "crosshair", fillColor: "#ffcc66", pointRadius: "10"});
        feature.mmsi = val.mmsi;
        feature.name = val.name;
        feature.type = val.type;
        feature.lat = val.lat;
        feature.lon = val.lon;
        feature.source = val;
        val.feature = feature;

        if (val.enabled) {
            feature.style = {
                pointRadius: "10", // sized according to type attribute
                fillColor: "#980000 ",
                strokeColor: "black",
                strokeWidth: 2,
                graphicZIndex: 1,
                cursor: "pointer"
            }
        } else {
            feature.style = {
                pointRadius: "10", // sized according to type attribute
                fillColor: "white",
                strokeColor: "black",
                strokeWidth: 2,
                graphicZIndex: 1,
                cursor: "pointer"
            }
        }
        if (self.selectedSource == val) {
            feature.style["strokeColor"] = "blue";
        } else {
            feature.style["strokeColor"] = "black";
        }

        sourceLayer.addFeatures(feature);
    }

//    this.loadSatStats = function(topleft, bottomright){
//    	
////    	var array = [{"fromTime":1374220785115,"toTime":1374220774501,"spanLength":1,"timeSinceLastSpan":0,"accumulatedTime":0,"signals":3,"distinctShips":1},{"fromTime":1374226711355,"toTime":1374227266676,"spanLength":9,"timeSinceLastSpan":98,"accumulatedTime":108,"signals":163,"distinctShips":7},{"fromTime":1374228081000,"toTime":1374228352000,"spanLength":4,"timeSinceLastSpan":13,"accumulatedTime":126,"signals":38,"distinctShips":6},{"fromTime":1374232884293,"toTime":1374233250297,"spanLength":6,"timeSinceLastSpan":75,"accumulatedTime":207,"signals":71,"distinctShips":6},{"fromTime":1374233909000,"toTime":1374234299000,"spanLength":6,"timeSinceLastSpan":10,"accumulatedTime":225,"signals":33,"distinctShips":7},{"fromTime":1374238552052,"toTime":1374239226409,"spanLength":11,"timeSinceLastSpan":70,"accumulatedTime":307,"signals":46,"distinctShips":6},{"fromTime":1374239872000,"toTime":1374239943000,"spanLength":1,"timeSinceLastSpan":10,"accumulatedTime":319,"signals":8,"distinctShips":5},{"fromTime":1374243070626,"toTime":1374243403490,"spanLength":5,"timeSinceLastSpan":52,"accumulatedTime":377,"signals":42,"distinctShips":6},{"fromTime":1374244812816,"toTime":1374245655000,"spanLength":14,"timeSinceLastSpan":23,"accumulatedTime":414,"signals":105,"distinctShips":6},{"fromTime":1374248972125,"toTime":1374249122001,"spanLength":2,"timeSinceLastSpan":55,"accumulatedTime":472,"signals":58,"distinctShips":7},{"fromTime":1374250432644,"toTime":1374251214883,"spanLength":13,"timeSinceLastSpan":21,"accumulatedTime":507,"signals":120,"distinctShips":7},{"fromTime":1374251846000,"toTime":1374251977000,"spanLength":2,"timeSinceLastSpan":10,"accumulatedTime":520,"signals":5,"distinctShips":4},{"fromTime":1374254798441,"toTime":1374255211329,"spanLength":6,"timeSinceLastSpan":47,"accumulatedTime":573,"signals":33,"distinctShips":6},{"fromTime":1374257362000,"toTime":1374257820000,"spanLength":7,"timeSinceLastSpan":35,"accumulatedTime":617,"signals":16,"distinctShips":7},{"fromTime":1374261058127,"toTime":1374261077807,"spanLength":1,"timeSinceLastSpan":53,"accumulatedTime":671,"signals":2,"distinctShips":1},{"fromTime":1374263221000,"toTime":1374263407000,"spanLength":3,"timeSinceLastSpan":35,"accumulatedTime":710,"signals":16,"distinctShips":5},{"fromTime":1374269013000,"toTime":1374269185000,"spanLength":2,"timeSinceLastSpan":93,"accumulatedTime":806,"signals":6,"distinctShips":2},{"fromTime":1374274821000,"toTime":1374274933000,"spanLength":1,"timeSinceLastSpan":93,"accumulatedTime":902,"signals":4,"distinctShips":4},{"fromTime":1374278293629,"toTime":1374278393470,"spanLength":1,"timeSinceLastSpan":56,"accumulatedTime":960,"signals":4,"distinctShips":3},{"fromTime":1374220785115,"toTime":1374220774501,"spanLength":1,"timeSinceLastSpan":33,"accumulatedTime":0,"signals":3,"distinctShips":1},{"fromTime":1374226711355,"toTime":1374227266676,"spanLength":9,"timeSinceLastSpan":98,"accumulatedTime":108,"signals":163,"distinctShips":7},{"fromTime":1374228081000,"toTime":1374228352000,"spanLength":4,"timeSinceLastSpan":13,"accumulatedTime":126,"signals":38,"distinctShips":6},{"fromTime":1374232884293,"toTime":1374233250297,"spanLength":6,"timeSinceLastSpan":75,"accumulatedTime":207,"signals":71,"distinctShips":6},{"fromTime":1374233909000,"toTime":1374234299000,"spanLength":6,"timeSinceLastSpan":10,"accumulatedTime":225,"signals":33,"distinctShips":7},{"fromTime":1374238552052,"toTime":1374239226409,"spanLength":11,"timeSinceLastSpan":70,"accumulatedTime":307,"signals":46,"distinctShips":6},{"fromTime":1374239872000,"toTime":1374239943000,"spanLength":1,"timeSinceLastSpan":10,"accumulatedTime":319,"signals":8,"distinctShips":5},{"fromTime":1374243070626,"toTime":1374243403490,"spanLength":5,"timeSinceLastSpan":52,"accumulatedTime":377,"signals":42,"distinctShips":6},{"fromTime":1374244812816,"toTime":1374245655000,"spanLength":14,"timeSinceLastSpan":23,"accumulatedTime":414,"signals":105,"distinctShips":6},{"fromTime":1374248972125,"toTime":1374249122001,"spanLength":2,"timeSinceLastSpan":55,"accumulatedTime":472,"signals":58,"distinctShips":7},{"fromTime":1374250432644,"toTime":1374251214883,"spanLength":13,"timeSinceLastSpan":21,"accumulatedTime":507,"signals":120,"distinctShips":7},{"fromTime":1374251846000,"toTime":1374251977000,"spanLength":2,"timeSinceLastSpan":10,"accumulatedTime":520,"signals":5,"distinctShips":4},{"fromTime":1374254798441,"toTime":1374255211329,"spanLength":6,"timeSinceLastSpan":47,"accumulatedTime":573,"signals":33,"distinctShips":6},{"fromTime":1374257362000,"toTime":1374257820000,"spanLength":7,"timeSinceLastSpan":35,"accumulatedTime":617,"signals":16,"distinctShips":7},{"fromTime":1374261058127,"toTime":1374261077807,"spanLength":1,"timeSinceLastSpan":53,"accumulatedTime":671,"signals":2,"distinctShips":1},{"fromTime":1374263221000,"toTime":1374263407000,"spanLength":3,"timeSinceLastSpan":35,"accumulatedTime":710,"signals":16,"distinctShips":5},{"fromTime":1374269013000,"toTime":1374269185000,"spanLength":2,"timeSinceLastSpan":93,"accumulatedTime":806,"signals":6,"distinctShips":2},{"fromTime":1374274821000,"toTime":1374274933000,"spanLength":1,"timeSinceLastSpan":93,"accumulatedTime":902,"signals":4,"distinctShips":4},{"fromTime":1374278293629,"toTime":1374379393470,"spanLength":1,"timeSinceLastSpan":56,"accumulatedTime":960,"signals":4,"distinctShips":3}];
//    	aisJsonClient.getSatCoverage(topleft.x+","+ topleft.y+","+ bottomright.x+","+ bottomright.y, function(array){
//    	
//	    	var output = "";
//	    	var outer = $("#outer");
//	//    	$("#bottomPanel").hide();
//	    	outer.html("");
//	    	result = "";
//	    	if(array.length == 0){
//	    		outer.html(" No data available");
//	    		console.log("NO DATA");
//	    		$("#bottomPanel").slideDown();
//    			return;
//    		}
//	//    	console.log($("#statusPanel").html());
//	    	var accumulatedTime = 0;
//	//     	alert(array[array.length-1].toTime);
//	    	lastDate = new Date(array[array.length-1].toTime);
//	    	
//	    	timeDifference = Math.ceil((array[array.length-1].toTime - array[0].fromTime)/1000/60/60);
//	    	
//	    	floorDate = new Date(array[0].fromTime);
//	    	floorDate.setMinutes(0);
//	    	offset = new Date(array[0].fromTime).getMinutes();
//	    	var accumulatedTime = offset;
//	    	var maxHeight = 0;
//	//     	alert(floorDate.getDate()+"-"+floorDate.getMonth());
//	
//	    	//set width of diagram
//	    	//outer.width(timeDifference*60);
//	    	if(timeDifference*60 < outer.width()){
//	    		timeDifference = outer.width()/60;
//	    	}
//	    	
//	    	//draw vertical lines
//	    	for (var i=0;i<=timeDifference;i++)
//	    	{ 
//	    		
//	    		if(i%3==0 || i==0){
//	    			currentDate = new Date(floorDate.getTime()+(1000*60*60*i));
//	//     			currentDate.setHours(currentDate.getHours()+i+1);
//	    			dateLabel = ('0' + currentDate.getDate()).slice(-2)+"-"+('0' + currentDate.getMonth()).slice(-2)+" "+(('0' + currentDate.getHours()).slice(-2)+":00");
//	//    			console.log(outer.html());
//	    			result+='<div class="labelDate" style="left:'+((i)*60-50)+'px">'+dateLabel+'</div>';
//	    		}
//	    		if(i == 0){
//	    			result+='<div class="leftVerticalLine" style="left:'+(i)*60+'px;"></div>';
//	    		}else{
//	    			result+='<div class="line" style="left:'+(i)*60+'px;"></div>';
//	    		}
//	    		
//	    	}
//	    	
//	    	//Draw the horizontal lower border
//	    	var lowerBorderwidth = 60*timeDifference;
//	    	if(lowerBorderwidth < outer.width()){
//	    		lowerBorderwidth=outer.width();
//	    	}
//	    	result+='<div class="horizontalLine" style="bottom:0px;left:0px;width:'+lowerBorderwidth+'px;"></div>';
//	    	//draw bottom horizontal line
//	    	
//	    	//Find max height and calculate scale factor
//	    	$.each(array, function(key, bar) {
//	    		if(bar.signals > maxHeight){maxHeight=bar.signals;}	  
//	    	});
//	    	if(maxHeight > outer.height()){
//	    	scale = outer.height()/maxHeight;
//	    	}else{
//	    		scale=1;
//	    	}
//	    	
//	    	//draw bars
//	    	$.each(array, function(key, bar) {
//	    		var leftPos =  (bar.fromTime - floorDate.getTime())/1000/60/60;
//	    		console.log(bar.fromTime+" "+bar.toTime);
//	    		if(key != 0){result+="<div class='rotate betweenSpanLabel' style='left: "+(accumulatedTime-50+(bar.timeSinceLastSpan/2))+"px'>"+bar.timeSinceLastSpan+" min</div>";}
//	    		accumulatedTime += bar.timeSinceLastSpan;
//	    		result+="<div class='bar' style='width: "+bar.spanLength+"px; height:"+bar.signals*scale+"px; background: #6E6E6E; left: "+accumulatedTime+"px;bottom:0px;'></div><div class='rotate timeSpanLabel' style='left: "+(accumulatedTime-50+(bar.spanLength/2))+"px'>"+bar.spanLength+" min</div><div class='value' style=' left: "+(accumulatedTime-50+(bar.spanLength/2))+"px;bottom:"+(bar.signals*scale)+"px;'>"+bar.signals+"</div>";
//	    		accumulatedTime += bar.spanLength;
//	    		  
//	    	});
//	    	
//	    	
//	    	outer.html(result);
//	    	$("#bottomPanel").slideDown();
//    	});
////    	alert(topleft);
//    }

    this.drawCoverage = function () {

        //get the multiplication factor for corresponding zoom level
        var multifactor = self.getMultiplicationFactor();
//    	multifactor = 5;
        $('#multiplicationFactor').html(multifactor);

        // activate loading panel
        $("#loadingPanel").css('visibility', 'visible');

        // get lat lon points for each screen corner
        var topleftpixel = new OpenLayers.Pixel(0, 0);
        var bottomrightpixel = new OpenLayers.Pixel($("#map").width(), $("#map").height());
        var topleftlonlat = map.getLonLatFromPixel(topleftpixel).transform(
            map.getProjectionObject(), // from Spherical Mercator Projection
            new OpenLayers.Projection("EPSG:4326") // to WGS 1984
        );
        var bottomrightlonlat = map.getLonLatFromPixel(bottomrightpixel).transform(
            map.getProjectionObject(), // from Spherical Mercator Projection
            new OpenLayers.Projection("EPSG:4326") // to WGS 1984
        );
        var screenarea = topleftlonlat.lat.toFixed(4) + "," + topleftlonlat.lon.toFixed(4) + "," + bottomrightlonlat.lat.toFixed(4) + "," + bottomrightlonlat.lon.toFixed(4);

        //convert enabled sources to string to be sent
        var dataToBeSent = self.enabledSourcesToString();


        //use json client to fetch data from web service
        aisJsonClient.getCoverage(dataToBeSent, screenarea, multifactor, selectedStartDate.getTime(), selectedEndDate.getTime(), function (data) {

            //remove existing cells
            polygonLayer.removeAllFeatures();

            $('#latSize').html(data.latSize.toFixed(4) + " degrees");
            $('#lonSize').html(data.lonSize.toFixed(4) + " degrees");


            var minExpectedMessages =
                $.each(data.cells, function (key, val) {
                    var points = [
                        new OpenLayers.Geometry.Point(val.lon, val.lat),
                        new OpenLayers.Geometry.Point(val.lon, val.lat + data.latSize),
                        new OpenLayers.Geometry.Point(val.lon + data.lonSize, val.lat + data.latSize),
                        new OpenLayers.Geometry.Point(val.lon + data.lonSize, val.lat)
                    ];
                    var totalMessages = 0;
                    var coverageValue = 0;

                    if (self.isVDMCategory()) {
                        totalMessages = (val.nrOfMisMes + val.nrOfRecMes);
                        coverageValue = val.nrOfRecMes / totalMessages;
                    } else {
                        totalMessages = val.numberOfVsiMessages;
                        coverageValue = val.averageSignalStrength;
                    }

                    computeThresholdValue = function (threshold) {
                        if (self.isVDMCategory()) {
                            return threshold / 100;
                        } else {
                            return threshold;
                        }
                    };

                    var color;
                    if (totalMessages >= self.minExpectedMessages) {
                        if (coverageValue >= computeThresholdValue(self.maxThreshold)) {
                            color = 'green';
                        } else if (coverageValue >= computeThresholdValue(self.minThreshold)) {
                            color = 'yellow';
                        } else {
                            color = 'red';
                        }

                        self.drawPolygon({
                            lat: val.lat,
                            lon: val.lon,
                            points: points,
                            fillcolor: color,
                            totalMessages: totalMessages,
                            receivedMessages: val.nrOfRecMes,
                            averageSignalStrength: val.averageSignalStrength
                        });
                    }
                });

//  		  self.loading = false;
            $("#loadingPanel").css('visibility', 'hidden');
        });
    }

    this.drawPolygon = function (options) {
        var site_points = [];
        for (i in options.points) {
            options.points[i].transform(
                new OpenLayers.Projection("EPSG:4326"), // transform from WGS 1984
                map.getProjectionObject() // to Spherical Mercator Projection
            );
            site_points.push(options.points[i]);
        }
        var linear_ring = new OpenLayers.Geometry.LinearRing(site_points);
        polygonFeature = new OpenLayers.Feature.Vector(
            new OpenLayers.Geometry.Polygon([linear_ring]), null);
        polygonFeature.attributes.fillcolor = options.fillcolor;
        polygonFeature.type = "cell";
        polygonFeature.lon = options.lon;
        polygonFeature.lat = options.lat;
        polygonFeature.totalMessages = options.totalMessages;
        polygonFeature.receivedMessages = options.receivedMessages;
        polygonFeature.averageSignalStrength = options.averageSignalStrength;
        polygonLayer.addFeatures([polygonFeature]);

    }

    this.enabledSourcesToString = function () {
        var string = "";
        $.each(this.sources, function (key, source) {
            if (source.enabled) {
                string += source.mmsi + ",";
            }
        });
        return string;
    }

    this.getMultiplicationFactor = function () {
        var zLevel = map.getZoom();
        var multifactor;
        if (zLevel > 10) {
            multifactor = 1;
        } else if (zLevel == 10) {
            multifactor = 1;
        } else if (zLevel == 9) {
            multifactor = 2;
        } else if (zLevel == 8) {
            multifactor = 3;
        } else if (zLevel == 7) {
            multifactor = 4;
        } else if (zLevel == 6) {
            multifactor = 8;
        } else if (zLevel == 5) {
            multifactor = 20;
        } else if (zLevel == 4) {
            multifactor = 40;
        } else if (zLevel == 3) {
            multifactor = 60;
        } else if (zLevel == 2) {
            multifactor = 60;
        } else if (zLevel == 1) {
            multifactor = 80;
        }
//    	alert(multifactor)
        return multifactor;
    }

    this.onFeatureSelect = function (evt) {

        feature = evt.feature;

        //determine if feature is a cell or a source
        if (feature.type == "cell") {

            //remove potential source
            self.selectedSource = null;
            self.drawSources();

            //Setting up cell details panel
            $("#featureDetailsPanel > .panelHeader").html("Cell Details");
            $("#featureDetailsPanel > .panelContainer").html(self.drawCellDetails(feature));
            $("#featureDetailsPanel").slideDown();


        } else {
            self.selectedSource = self.sources[feature.mmsi];
            self.refreshSourceDetails();
            $("#featureDetailsPanel").slideDown();
            $("#featureDetailsPanel > .panelHeader").html("Source Details");
            self.drawSources();
        }

    }

    this.drawCellDetails = function (feature) {
        var cellDetail =
            '<div class="smallText">Source</div>' +
            '<div class="information">' + feature.mmsi + '</div>' +
            '<div class="smallText">Cell Latitude</div>' +
            '<div class="information">' + feature.lat.toFixed(4) + '</div>' +
            '<div class="smallText">Cell Longitude</div>' +
            '<div class="information">' + feature.lon.toFixed(4) + '</div>';

        if (self.isVDMCategory()) {
            cellDetail = cellDetail.concat(
                '<div class="smallText">Received Messages</div>' +
                '<div class="information">' + feature.receivedMessages + '</div>' +
                '<div class="smallText">Expected Messages</div>' +
                '<div class="information">' + feature.totalMessages + '</div>' +
                '<div class="smallText">Coverage</div>' +
                '<div class="information">' + ((feature.receivedMessages / feature.totalMessages) * 100).toFixed(2) + ' ' + self.thresholdUnit + '</div>');
        } else {
            cellDetail = cellDetail.concat(
                '<div class="smallText">Number of Messages</div>' +
                '<div class="information">' + feature.totalMessages + '</div>' +
                '<div class="smallText">Average Signal Strength</div>' +
                '<div class="information">' + feature.averageSignalStrength + ' ' + self.thresholdUnit + '</div>');
        }

        return cellDetail;
    }

    this.cleanupCellDetails = function () {
        $("#featureDetailsPanel > .panelHeader").html("Cell Details");
        $("#featureDetailsPanel > .panelContainer").html("");
        $("#featureDetailsPanel").slideUp();
    }

    this.onFeatureUnselect = function (evt) {

//    	feature = evt.feature;
//    	
//    	//determine if feature is a cell or a source
//    	if(feature.type == "cell"){
//
//        }
////    	else{
//        	self.selectedSource = null;
////        	$("#featureDetailsPanel").slideUp();
//        	self.drawSources();
////        }

    }

}
