// This file is part of OpenTSDB.
// Copyright (C) 2010-2012  The OpenTSDB Authors.
//
// This program is free software: you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 2.1 of the License, or (at your
// option) any later version.  This program is distributed in the hope that it
// will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
// General Public License for more details.  You should have received a copy
// of the GNU Lesser General Public License along with this program.  If not,
// see <http://www.gnu.org/licenses/>.
package net.opentsdb.core;

import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Utility class that provides common, generally useful aggregators.
 */
public final class Aggregators {

  /**
   * Different interpolation methods
   */
  public enum Interpolation {
    LERP,   /* Regular linear interpolation */
    ZIM,    /* Returns 0 when a data point is missing */
    MAX,    /* Returns the <type>.MaxValue when a data point is missing */
    MIN     /* Returns the <type>.MinValue when a data point is missing */
  }
  
  /** Aggregator that sums up all the data points. */
  public static final Aggregator SUM = new Sum(
      Interpolation.LERP, "sum");

  /** Aggregator that returns the minimum data point. */
  public static final Aggregator MIN = new Min(
      Interpolation.LERP, "min");

  /** Aggregator that returns the maximum data point. */
  public static final Aggregator MAX = new Max(
      Interpolation.LERP, "max");

  /** Aggregator that returns the average value of the data point. */
  public static final Aggregator AVG = new Avg(
      Interpolation.LERP, "avg");

  /** Aggregator that returns the Standard Deviation of the data points. */
  public static final Aggregator DEV = new StdDev(
      Interpolation.LERP, "dev");
  
  /** Sums data points but will cause the SpanGroup to return a 0 if timesamps
   * don't line up instead of interpolating. */
  public static final Aggregator ZIMSUM = new Sum(
      Interpolation.ZIM, "zimsum");

  /** Returns the minimum data point, causing SpanGroup to set <type>.MaxValue
   * if timestamps don't line up instead of interpolating. */
  public static final Aggregator MIMMIN = new Min(
      Interpolation.MAX, "mimmin");
  
  /** Returns the maximum data point, causing SpanGroup to set <type>.MinValue
   * if timestamps don't line up instead of interpolating. */
  public static final Aggregator MIMMAX = new Max(
      Interpolation.MIN, "mimmax");
  
  public static final Aggregator MED = new Med(
      Interpolation.LERP, "med");
  
  public static final Aggregator P95 = new P95(
      Interpolation.LERP, "p95");

  public static final Aggregator P99 = new P99(
      Interpolation.LERP, "p99");

  public static final Aggregator PERCENT0 = new Percent0(
      Interpolation.LERP, "percent0");

  public static final Aggregator PERCENT1 = new Percent1(
      Interpolation.LERP, "percent1");

  public static final Aggregator PERCENT2 = new Percent2(
      Interpolation.LERP, "percent2");

  /** Maps an aggregator name to its instance. */
  private static final HashMap<String, Aggregator> aggregators;

  static {
    aggregators = new HashMap<String, Aggregator>(8);
    aggregators.put("sum", SUM);
    aggregators.put("min", MIN);
    aggregators.put("max", MAX);
    aggregators.put("avg", AVG);
    aggregators.put("dev", DEV);
    aggregators.put("zimsum", ZIMSUM);
    aggregators.put("mimmin", MIMMIN);
    aggregators.put("mimmax", MIMMAX);
    aggregators.put("med", MED);
    aggregators.put("p95", P95);
    aggregators.put("p99", P99);
    aggregators.put("percent0", PERCENT0);
    aggregators.put("percent1", PERCENT1);
    aggregators.put("percent2", PERCENT2);
  }

  private Aggregators() {
    // Can't create instances of this utility class.
  }

  /**
   * Returns the set of the names that can be used with {@link #get get}.
   */
  public static Set<String> set() {
    return aggregators.keySet();
  }

  /**
   * Returns the aggregator corresponding to the given name.
   * @param name The name of the aggregator to get.
   * @throws NoSuchElementException if the given name doesn't exist.
   * @see #set
   */
  public static Aggregator get(final String name) {
    final Aggregator agg = aggregators.get(name);
    if (agg != null) {
      return agg;
    }
    throw new NoSuchElementException("No such aggregator: " + name);
  }

  private static final class Sum implements Aggregator {
    private final Interpolation method;
    private final String name;
    
    public Sum(final Interpolation method, final String name) {
      this.method = method;
      this.name = name;
    }
    
    public long runLong(final Longs values) {
      long result = values.nextLongValue();
      while (values.hasNextValue()) {
        result += values.nextLongValue();
      }
      return result;
    }

    public double runDouble(final Doubles values) {
      double result = values.nextDoubleValue();
      while (values.hasNextValue()) {
        result += values.nextDoubleValue();
      }
      return result;
    }

    public String toString() {
      return name;
    }

    public Interpolation interpolationMethod() {
      return method;
    }
    
  }

  private static final class Med implements Aggregator {
    private final Interpolation method;
    private final String name;

    public Med(final Interpolation method, final String name) {
      this.method = method;
      this.name = name;
    }

    public long runLong(final Longs values) {
      int SIZE = 10000;
      int seen = 0;
      double[] sample = new double[SIZE];
      while (values.hasNextValue()) {
        final double val = values.nextLongValue();
        if (seen < SIZE) {
          sample[seen++] = val;
        } else {
          int r = (int) (++seen * Math.random());
          if (r < SIZE) {
            sample[r] = val;
          }
        }
      }
      if (seen < SIZE) {
        double[] smallsample = new double[seen];
        for (int i = 0; i < seen; i++) {
          smallsample[i] = sample[i];
        }
        sample = smallsample;
      }
      java.util.Arrays.sort(sample);
      if (seen == 0) return 0;
      if (sample.length % 2 == 0) return (long) (sample[sample.length/2] + sample[sample.length/2-1]) / 2;
      else return (long) sample[sample.length/2];
    }

    public double runDouble(final Doubles values) {
      int SIZE = 10000;
      int seen = 0;
      double[] sample = new double[SIZE];
      while (values.hasNextValue()) {
        final double val = values.nextDoubleValue();
        if (seen < SIZE) {
          sample[seen++] = val;
        } else {
          int r = (int) (++seen * Math.random());
          if (r < SIZE) {
            sample[r] = val;
          }
        }
      }
      if (seen < SIZE) {
        double[] smallsample = new double[seen];
        for (int i = 0; i < seen; i++) {
          smallsample[i] = sample[i];
        }
        sample = smallsample;
      }
      java.util.Arrays.sort(sample);
      if (seen == 0) return 0;
      if (sample.length % 2 == 0) return (sample[sample.length/2] + sample[sample.length/2-1]) / 2;
      else return sample[sample.length/2];
    }

    public String toString() {
      return name;
    }

    public Interpolation interpolationMethod() {
      return method;
    }
  }
  private static final class P95 implements Aggregator {
    private final Interpolation method;
    private final String name;

    public P95(final Interpolation method, final String name) {
      this.method = method;
      this.name = name;
    }

    public long runLong(final Longs values) {
      int SIZE = 10000;
      int seen = 0;
      double[] sample = new double[SIZE];
      while (values.hasNextValue()) {
        final double val = values.nextLongValue();
        if (seen < SIZE) {
          sample[seen++] = val;
        } else {
          int r = (int) (++seen * Math.random());
          if (r < SIZE) {
            sample[r] = val;
          }
        }
      }
      if (seen < SIZE) {
        double[] smallsample = new double[seen];
        for (int i = 0; i < seen; i++) {
          smallsample[i] = sample[i];
        }
        sample = smallsample;
      }
      java.util.Arrays.sort(sample);
      return (long) sample[(int)(sample.length*.95)];
    }

    public double runDouble(final Doubles values) {
      int SIZE = 10000;
      int seen = 0;
      double[] sample = new double[SIZE];
      while (values.hasNextValue()) {
        final double val = values.nextDoubleValue();
        if (seen < SIZE) {
          sample[seen++] = val;
        } else {
          int r = (int) (++seen * Math.random());
          if (r < SIZE) {
            sample[r] = val;
          }
        }
      }
      if (seen < SIZE) {
        double[] smallsample = new double[seen];
        for (int i = 0; i < seen; i++) {
          smallsample[i] = sample[i];
        }
        sample = smallsample;
      }
      java.util.Arrays.sort(sample);
      return sample[(int)(sample.length*.95)];
    }

    public String toString() {
      return name;
    }

    public Interpolation interpolationMethod() {
      return method;
    }
  }

  private static final class P99 implements Aggregator {
    private final Interpolation method;
    private final String name;

    public P99(final Interpolation method, final String name) {
      this.method = method;
      this.name = name;
    }

    public long runLong(final Longs values) {
      int SIZE = 10000;
      int seen = 0;
      double[] sample = new double[SIZE];
      while (values.hasNextValue()) {
        final double val = values.nextLongValue();
        if (seen < SIZE) {
          sample[seen++] = val;
        } else {
          int r = (int) (++seen * Math.random());
          if (r < SIZE) {
            sample[r] = val;
          }
        }
      }
      if (seen < SIZE) {
        double[] smallsample = new double[seen];
        for (int i = 0; i < seen; i++) {
          smallsample[i] = sample[i];
        }
        sample = smallsample;
      }
      java.util.Arrays.sort(sample);
      return (long) sample[(int)(sample.length*.99)];
    }

    public double runDouble(final Doubles values) {
      int SIZE = 10000;
      int seen = 0;
      double[] sample = new double[SIZE];
      while (values.hasNextValue()) {
        final double val = values.nextDoubleValue();
        if (seen < SIZE) {
          sample[seen++] = val;
        } else {
          int r = (int) (++seen * Math.random());
          if (r < SIZE) {
            sample[r] = val;
          }
        }
      }
      if (seen < SIZE) {
        double[] smallsample = new double[seen];
        for (int i = 0; i < seen; i++) {
          smallsample[i] = sample[i];
        }
        sample = smallsample;
      }
      java.util.Arrays.sort(sample);
      return sample[(int)(sample.length*.99)];
    }

    public String toString() {
      return name;
    }

    public Interpolation interpolationMethod() {
      return method;
    }
  }

  private static final class Min implements Aggregator {
    private final Interpolation method;
    private final String name;
    
    public Min(final Interpolation method, final String name) {
      this.method = method;
      this.name = name;
    }
    
    public long runLong(final Longs values) {
      long min = values.nextLongValue();
      while (values.hasNextValue()) {
        final long val = values.nextLongValue();
        if (val < min) {
          min = val;
        }
      }
      return min;
    }

    public double runDouble(final Doubles values) {
      double min = values.nextDoubleValue();
      while (values.hasNextValue()) {
        final double val = values.nextDoubleValue();
        if (val < min) {
          min = val;
        }
      }
      return min;
    }

    public String toString() {
      return name;
    }

    public Interpolation interpolationMethod() {
      return method;
    }
    
  }

  private static final class Max implements Aggregator {
    private final Interpolation method;
    private final String name;
    
    public Max(final Interpolation method, final String name) {
      this.method = method;
      this.name = name;
    }
    
    public long runLong(final Longs values) {
      long max = values.nextLongValue();
      while (values.hasNextValue()) {
        final long val = values.nextLongValue();
        if (val > max) {
          max = val;
        }
      }
      return max;
    }

    public double runDouble(final Doubles values) {
      double max = values.nextDoubleValue();
      while (values.hasNextValue()) {
        final double val = values.nextDoubleValue();
        if (val > max) {
          max = val;
        }
      }
      return max;
    }

    public String toString() {
      return name;
    }

    public Interpolation interpolationMethod() {
      return method;
    }
    
  }

  private static final class Avg implements Aggregator {
    private final Interpolation method;
    private final String name;
    
    public Avg(final Interpolation method, final String name) {
      this.method = method;
      this.name = name;
    }
    
    public long runLong(final Longs values) {
      long result = values.nextLongValue();
      int n = 1;
      while (values.hasNextValue()) {
        result += values.nextLongValue();
        n++;
      }
      return result / n;
    }

    public double runDouble(final Doubles values) {
      double result = values.nextDoubleValue();
      int n = 1;
      while (values.hasNextValue()) {
        result += values.nextDoubleValue();
        n++;
      }
      return result / n;
    }

    public String toString() {
      return name;
    }
  
    public Interpolation interpolationMethod() {
      return method;
    }
   
  }

  /**
   * Standard Deviation aggregator.
   * Can compute without storing all of the data points in memory at the same
   * time.  This implementation is based upon a
   * <a href="http://www.johndcook.com/standard_deviation.html">paper by John
   * D. Cook</a>, which itself is based upon a method that goes back to a 1962
   * paper by B.  P. Welford and is presented in Donald Knuth's Art of
   * Computer Programming, Vol 2, page 232, 3rd edition
   */
  private static final class StdDev implements Aggregator {
    private final Interpolation method;
    private final String name;
    
    public StdDev(final Interpolation method, final String name) {
      this.method = method;
      this.name = name;
    }
    
    public long runLong(final Longs values) {
      double old_mean = values.nextLongValue();

      if (!values.hasNextValue()) {
        return 0;
      }

      long n = 2;
      double new_mean = 0;
      double variance = 0;
      do {
        final double x = values.nextLongValue();
        new_mean = old_mean + (x - old_mean) / n;
        variance += (x - old_mean) * (x - new_mean);
        old_mean = new_mean;
        n++;
      } while (values.hasNextValue());

      return (long) Math.sqrt(variance / (n - 1));
    }

    public double runDouble(final Doubles values) {
      double old_mean = values.nextDoubleValue();

      if (!values.hasNextValue()) {
        return 0;
      }

      long n = 2;
      double new_mean = 0;
      double variance = 0;
      do {
        final double x = values.nextDoubleValue();
        new_mean = old_mean + (x - old_mean) / n;
        variance += (x - old_mean) * (x - new_mean);
        old_mean = new_mean;
        n++;
      } while (values.hasNextValue());

      return Math.sqrt(variance / (n - 1));
    }

    public String toString() {
      return name;
    }
    
    public Interpolation interpolationMethod() {
      return method;
    }
    
  }

  private static final class Percent0 implements Aggregator {
    private final Interpolation method;
    private final String name;

    public Percent0(final Interpolation method, final String name) {
      this.method = method;
      this.name = name;
    }

    public long runLong(final Longs values) {
      long total = 0;
      long numZeros = 0;
      do {
        final int x = (int) values.nextLongValue();
        total++;
        if (x == 0) numZeros++;
      } while (values.hasNextValue());
      return 100 * numZeros / total;
    }

    public double runDouble(final Doubles values) {
      long total = 0;
      long numZeros = 0;
      do {
        final int x = (int) values.nextDoubleValue();
        total++;
        if (x == 0) numZeros++;
      } while (values.hasNextValue());
      return 100 * numZeros / (double) total;
    }

    public String toString() {
      return name;
    }

    public Interpolation interpolationMethod() {
      return method;
    }

  }

  private static final class Percent1 implements Aggregator {
    private final Interpolation method;
    private final String name;

    public Percent1(final Interpolation method, final String name) {
      this.method = method;
      this.name = name;
    }

    public long runLong(final Longs values) {
      long total = 0;
      long numOnes = 0;
      do {
        final int x = (int) values.nextLongValue();
        total++;
        if (x == 1) numOnes++;
      } while (values.hasNextValue());
      return 100 * numOnes / total;
    }

    public double runDouble(final Doubles values) {
      long total = 0;
      long numOnes = 0;
      do {
        final int x = (int) values.nextDoubleValue();
        total++;
        if (x == 1) numOnes++;
      } while (values.hasNextValue());
      return 100 * numOnes / (double) total;
    }

    public String toString() {
      return name;
    }

    public Interpolation interpolationMethod() {
      return method;
    }

  }

  private static final class Percent2 implements Aggregator {
    private final Interpolation method;
    private final String name;

    public Percent2(final Interpolation method, final String name) {
      this.method = method;
      this.name = name;
    }

    public long runLong(final Longs values) {
      long total = 0;
      long numTwos = 0;
      do {
        final int x = (int) values.nextLongValue();
        total++;
        if (x == 2) numTwos++;
      } while (values.hasNextValue());
      return 100 * numTwos / total;
    }

    public double runDouble(final Doubles values) {
      long total = 0;
      long numTwos = 0;
      do {
        final int x = (int) values.nextDoubleValue();
        total++;
        if (x == 1) numTwos++;
      } while (values.hasNextValue());
      return 100 * numTwos / (double) total;
    }

    public String toString() {
      return name;
    }

    public Interpolation interpolationMethod() {
      return method;
    }

  }
}
