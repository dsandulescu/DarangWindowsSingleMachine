

import java.awt.Color;
import java.util.Vector;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

/**
 * A simple demonstration application showing how to create a line chart using data from an
 * {@link XYDataset}.
 *
 */
public class ChartCreater extends ApplicationFrame {



	/**
	 * Creates a new demo.
	 *
	 * @param are the title for the chart and the data for the chart. In this we have 
	 * observed data and simulated data
	 * 
	 */
	public ChartCreater(final String title, final Vector<Double> flowData, final  Vector<Double> flowDataObserved ) {

		super(title);

		final XYDataset dataset = createDataset( flowData, flowDataObserved);
		final JFreeChart chart = createChart(dataset, title);
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
		setContentPane(chartPanel);
		
		
		this.pack();
		RefineryUtilities.centerFrameOnScreen(this);
		this.setVisible(true);
		

	}

	/**
	 * Creates a dataset using from the series(Observed and simulated data)
	 * 
	 * @return .
	 */
	private XYDataset createDataset(final Vector<Double> flowData, final Vector<Double> flowDataObserved) {

		final XYSeries series1 = new XYSeries("flowDataObserved");

		for(int i = 0; i < flowDataObserved.size(); i++ )
		{
			series1.add(i, flowDataObserved.get(i));
		}
		final XYSeries series2 = new XYSeries("flowDataPredicted");
		
		for(int i = 0; i < flowData.size(); i++ )
		{
			series2.add(i, flowData.get(i));
		}

		final XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(series1);
		dataset.addSeries(series2);


		return dataset;

	}

	/**
	 * Creates a chart.
	 * 
	 * @param dataset  the data for the chart.
	 * 
	 * @return a chart.
	 */
	private JFreeChart createChart(final XYDataset dataset, final String title) {

		// create the chart...
		final JFreeChart chart = ChartFactory.createXYLineChart(
				title,      // chart title
				"X",                      // x axis label
				"Y",                      // y axis label
				dataset,                  // data
				PlotOrientation.VERTICAL,
				true,                     // include legend
				true,                     // tooltips
				false                     // urls
				);

		// NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
		chart.setBackgroundPaint(Color.white);

		//            final StandardLegend legend = (StandardLegend) chart.getLegend();
		//      legend.setDisplaySeriesShapes(true);

		// get a reference to the plot for further customisation...
		final XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.lightGray);
		//    plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);

		final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setSeriesLinesVisible(0, false);
		renderer.setSeriesShapesVisible(1, false);
		plot.setRenderer(renderer);

		// change the auto tick unit selection to integer units only...
		final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		// OPTIONAL CUSTOMISATION COMPLETED.

		return chart;

	}

}