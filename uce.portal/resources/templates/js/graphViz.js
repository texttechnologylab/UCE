/**
 * Wrapper class for multiple visualization options.\
 * THIS NEEDS TO IMPORT THE LOCAL CHARTJS LIB IN THE HEADER:
 *  <script src="js/visualization/cdns/chartjs-449.js"></script>
 */
import { ChartJS } from '/js/visualization/chartjs.js';

const chartJs = new ChartJS();
console.log(chartJs.drawChart());