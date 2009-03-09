package scalanlp.optimize;

import scala.collection.mutable._;
import util._;
import util.Implicits._;
import stats.sampling._;

/*
 Copyright 2009 David Hall, Daniel Ramage
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at 
 
 http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License. 
*/

/** 
* Minimizes a function using stochastic gradient descent
*
* @author dlwh
*/
class StochasticGradientDescent(val alpha: Double,
    val scale: Double, 
    val tol: Double,
    val mxIter: Int,
    batchSize: Int) extends Minimizer[Seq[Double],BatchDiffFunction[Seq[Double]]] with Logged {

  /**
  * Runs SGD on f, for mxIter. It ignores tol.
  */
  def minimize(f: BatchDiffFunction[Seq[Double]], init: Seq[Double]) = {
    val maxIter = if(mxIter <= 0) {
      1000 * f.fullRange.size / batchSize;
    } else {
      mxIter * f.fullRange.size / batchSize;
    }

    var temp = 1.0;
    var guess = init;

    log(Log.INFO)("SGD starting");
    log(Log.INFO)("SGD init: " + init);
    log(Log.INFO)("SGD maxIter: " + maxIter);

    for(i <- 1 to maxIter) {
      log(Log.INFO)("SGD iteration: " + i);
      val sample = selectSample(f,i);

      val grad = f.gradientAt(guess,sample);
      log(Log.INFO)("SGD gradient: " + grad);
      assert(grad.forall(!_.isInfinite));

      guess = update(guess,grad,temp);
      assert(guess.forall(!_.isInfinite));
      log(Log.INFO)("SGD update: " + guess);

      temp = alpha/(alpha + i); 
      log(Log.INFO)("SGD temp: " + temp);
    }
    guess;
  }

  /**
  * Applies the update given the gradient. By default, it executes:
  *
  * guess -= temp * grad;
  */
  def update(guess: Seq[Double], grad: Seq[Double], temp: Double):Seq[Double] = {
    guess.elements.zip(grad.elements).map( tup => tup._1 - temp * tup._2).collect;
  }

  /**
  * Selects a sample of the data to evalute on. By default, it selects
  * a random sample without replacement.
  */
  def selectSample(f: BatchDiffFunction[Seq[Double]], iter: Int) : Seq[Int] = {
    Rand.permutation(f.fullRange.size).draw.map(f.fullRange).take(batchSize);
  }
}