package org.rapidpm.demo.iot.vaadin;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import com.vaadin.addon.charts.Chart;
import com.vaadin.addon.charts.model.*;
import com.vaadin.addon.charts.model.style.Color;
import com.vaadin.addon.charts.model.style.GradientColor;
import com.vaadin.addon.charts.model.style.SolidColor;
import com.vaadin.addon.timeline.Timeline;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.sass.internal.parser.ParseException;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 */
//@Theme("mytheme")
//@Widgetset("org.rapidpm.demo.iot.vaadin.MyAppWidgetset")
@Push(value = PushMode.AUTOMATIC)
public class MyUI extends UI {

    public static final String SERVER_URI = "tcp://127.0.0.1:1883";

    @WebServlet(value = "/*", asyncSupported = true, initParams = {
            @WebInitParam(name = "pushmode", value = "automatic")
    })
    @VaadinServletConfiguration(productionMode = false, ui = MyUI.class, widgetset = "org.rapidpm.demo.iot.vaadin.AppWidgetSet")
    public static class Servlet extends VaadinServlet {
    }

    public static final String TOPIC = "TinkerForge/Wetterstation/";

    public static final String TOPIC_Light = "TinkerForge/Wetterstation/Light";
    public static final String TOPIC_Temp = "TinkerForge/Wetterstation/Temp";
    public static final String TOPIC_Hum = "TinkerForge/Wetterstation/Hum";
    public static final String TOPIC_AirPressure = "TinkerForge/Wetterstation/Air";

    //    private String lastMessage = "start:-:-:0";
    private Chart chartAmbientLight = (Chart) getChart("Ambient Light", "512px", 0, 100, " Lux");
    private Chart chartTemperature = (Chart) getChart("Temperature", "512px", 23, 25, " C");
    private Chart chartHumidity = (Chart) getChart("Humidity", "512px", 28, 33, " %RH");
    private Chart chartAirpressure = (Chart) getChart("Air Pressure", "512px", 100, 1_100, " mBar");

    @Override
    protected void init(VaadinRequest request) {
        final VerticalLayout layout = new VerticalLayout();
        layout.setWidth("2048px");
        layout.setMargin(true);
//        layout.setSizeUndefined();
        setContent(layout);

        HorizontalLayout horizontalLayout = new HorizontalLayout();
//        horizontalLayout.setMargin(true);
        horizontalLayout.setSizeUndefined();
        layout.addComponent(horizontalLayout);

        horizontalLayout.addComponent(chartAmbientLight);
        horizontalLayout.addComponent(chartTemperature);
        horizontalLayout.addComponent(chartHumidity);
        horizontalLayout.addComponent(chartAirpressure);

        Timeline timeline = new Timeline("Our timeline");
        timeline.setWidth("100%");
        timeline.setChartModesVisible(false);

        timeline.addZoomLevel("Min", 86400000L * 24 * 60);
        timeline.addZoomLevel("Hour", 86400000L * 24);
        timeline.addZoomLevel("Day", 86400000L);

        final Container.Indexed graphDataSource = createGraphDataSource();
        Container.Indexed markerDataSource = createMarkerDataSource();

        timeline.addGraphDataSource(graphDataSource,
                Timeline.PropertyId.TIMESTAMP,
                Timeline.PropertyId.VALUE);

        timeline.setMarkerDataSource(markerDataSource,
                Timeline.PropertyId.TIMESTAMP,
                Timeline.PropertyId.CAPTION,
                Timeline.PropertyId.VALUE);
//
//      timeline.setEventDataSource(createEventDataSource(),
//              Timeline.PropertyId.TIMESTAMP,
//              Timeline.PropertyId.CAPTION);

        layout.addComponent(timeline);

        try {
            MqttClient empfLight = new MqttClient(SERVER_URI, "ClientAmbietLight", new MemoryPersistence());
            empfLight.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                }

                @Override
                public void messageArrived(String str, MqttMessage mqttMessage) throws Exception {
                    byte[] payload = mqttMessage.getPayload();
                    String lastMessage = new String(payload);
                    access(() -> chartAmbientLight.getConfiguration()
                            .getSeries()
                            .forEach(s -> {
                                Double newValue = Double.valueOf(lastMessage.split(":")[3]);
                                ((ListSeries) s).updatePoint(0, newValue);
                                Date date = new Date();// not 100% right
                                Item itemGraph = graphDataSource.addItem(date);
                                // Set the timestamp property
                                itemGraph.getItemProperty(Timeline.PropertyId.TIMESTAMP).setValue(date);
                                // Set the value property
                                itemGraph.getItemProperty(Timeline.PropertyId.VALUE).setValue(newValue);

                                Item itemMarker = markerDataSource.addItem(date);
                                // Set the timestamp property
                                itemMarker.getItemProperty(Timeline.PropertyId.TIMESTAMP).setValue(date);
                                // Set the caption property
                                itemMarker.getItemProperty(Timeline.PropertyId.CAPTION).setValue("x");
                                // Set the value property
                                itemMarker.getItemProperty(Timeline.PropertyId.VALUE).setValue("Timestamp  " + date.getTime());

                            }));
                    System.out.println("pushed -> lastMessage = " + lastMessage);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                }
            });
            empfLight.connect();
            empfLight.subscribe(TOPIC_Light, 1);

            MqttClient empfTemp = new MqttClient(SERVER_URI, "ClientTemperature", new MemoryPersistence());
            empfTemp.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                }

                @Override
                public void messageArrived(String str, MqttMessage mqttMessage) throws Exception {
                    byte[] payload = mqttMessage.getPayload();
                    String lastMessage = new String(payload);
                    access(() -> chartTemperature.getConfiguration()
                            .getSeries()
                            .forEach(s -> {
                                Double newValue = Double.valueOf(lastMessage.split(":")[3]);
                                ((ListSeries) s).updatePoint(0, newValue);
                            }));
                    System.out.println("pushed -> lastMessage = " + lastMessage);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                }
            });
            empfTemp.connect();
            empfTemp.subscribe(TOPIC_Temp, 1);

            MqttClient empfAirpressure = new MqttClient(SERVER_URI, "ClientAirpressure", new MemoryPersistence());
            empfAirpressure.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                }

                @Override
                public void messageArrived(String str, MqttMessage mqttMessage) throws Exception {
                    byte[] payload = mqttMessage.getPayload();
                    String lastMessage = new String(payload);
                    access(() -> chartAirpressure.getConfiguration()
                            .getSeries()
                            .forEach(s -> {
                                Double newValue = Double.valueOf(lastMessage.split(":")[3]);
                                ((ListSeries) s).updatePoint(0, newValue);
                            }));
                    System.out.println("pushed -> lastMessage = " + lastMessage);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                }
            });
            empfAirpressure.connect();
            empfAirpressure.subscribe(TOPIC_AirPressure, 1);

            MqttClient empfHumidity = new MqttClient(SERVER_URI, "ClientHumidity", new MemoryPersistence());
            empfHumidity.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                }

                @Override
                public void messageArrived(String str, MqttMessage mqttMessage) throws Exception {
                    byte[] payload = mqttMessage.getPayload();
                    String lastMessage = new String(payload);
                    access(() -> chartHumidity.getConfiguration()
                            .getSeries()
                            .forEach(s -> {
                                Double newValue = Double.valueOf(lastMessage.split(":")[3]);
                                ((ListSeries) s).updatePoint(0, newValue);
                            }));
                    System.out.println("pushed -> lastMessage = " + lastMessage);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                }
            });
            empfHumidity.connect();
            empfHumidity.subscribe(TOPIC_Hum, 1);

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    /**
     * Creates a graph container with a month of random data
     */
    public Container.Indexed createGraphDataSource() {
        // Create the container
        Container.Indexed container = new IndexedContainer();
        // Add the required property ids (use the default ones here)
        container.addContainerProperty(Timeline.PropertyId.TIMESTAMP, Date.class, null);
        container.addContainerProperty(Timeline.PropertyId.VALUE, Double.class, 0);
        return container;
    }


    /**
     * Creates a marker container with a marker for each seven days
     */
    public Container.Indexed createMarkerDataSource() {
        // Create the container
        Container.Indexed container = new IndexedContainer();
        // Add the required property IDs (use the default ones here)
        container.addContainerProperty(Timeline.PropertyId.TIMESTAMP, Date.class, null);
        container.addContainerProperty(Timeline.PropertyId.CAPTION, String.class, "Our marker symbol");
        container.addContainerProperty(Timeline.PropertyId.VALUE, String.class, "Our description");

        return container;
    }


    protected Component getChart(String title, String width, int min, int max, String unit) {
        final Chart chart = new Chart();
        chart.setWidth(width);

        final Configuration configuration = new Configuration();
        configuration.getChart().setType(ChartType.GAUGE);
        configuration.getChart().setAlignTicks(false);
        configuration.getChart().setPlotBackgroundColor(null);
        configuration.getChart().setPlotBackgroundImage(null);
        configuration.getChart().setPlotBorderWidth(0);
        configuration.getChart().setPlotShadow(false);
        configuration.setTitle(title);

        configuration.getPane().setStartAngle(-150);
        configuration.getPane().setEndAngle(150);

        YAxis yAxis = new YAxis();

        yAxis.setMin(min);
        yAxis.setMax(max);
        yAxis.setLineColor(new SolidColor("#339"));
        yAxis.setTickColor(new SolidColor("#339"));
        yAxis.setMinorTickColor(new SolidColor("#339"));
        yAxis.setOffset(-25);
        yAxis.setLineWidth(2);
        yAxis.setLabels(new Labels());
        yAxis.getLabels().setDistance(-20);
        yAxis.getLabels().setRotationPerpendicular();
        yAxis.setTickLength(5);
        yAxis.setMinorTickLength(5);
        yAxis.setEndOnTick(false);

        configuration.addyAxis(yAxis);

        final ListSeries series = new ListSeries(title, 12);

        PlotOptionsGauge plotOptionsGauge = new PlotOptionsGauge();
        plotOptionsGauge.setDataLabels(new Labels());
        plotOptionsGauge.getDataLabels().setFormatter("function() {return '' + this.y +  ' " + unit + "';}");
        GradientColor gradient = GradientColor.createLinear(0, 0, 0, 1);
        gradient.addColorStop(0, new SolidColor("#DDD"));
        gradient.addColorStop(1, new SolidColor("#FFF"));
        plotOptionsGauge.getDataLabels().setBackgroundColor(gradient);
        plotOptionsGauge.getTooltip().setValueSuffix(unit);
        series.setPlotOptions(plotOptionsGauge);
        configuration.setSeries(series);
        chart.drawChart(configuration);

        return chart;
    }
}
