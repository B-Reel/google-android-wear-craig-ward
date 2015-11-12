package com.breel.wearables.shadowclock.graphics;

/**
 * Class to get a Gaussian function
 */
public final class Gaussian {

    // return phi(x) = standard Gaussian PDF
    public static double phi(double x) {
        return Math.exp(-x * x / 2) / Math.sqrt(2 * Math.PI);
    }


    // return Phi(z) = standard Gaussian CDF using Taylor approximation
    public static double Phi(double z) {
        if (z < -8.0) return 0.0;
        if (z > 8.0) return 1.0;
        double sum = 0.0, term = z;
        for (int i = 3; sum + term != sum; i += 2) {
            sum = sum + term;
            term = term * z * z / i;
        }
        return 0.5 + sum * phi(z);
    }


    // return Phi(z, mu, sigma) = Gaussian CDF with mean mu and stddev sigma
    public static double Phi(double z, double mu, double sigma) {
        return Phi((z - mu) / sigma);
    }


    public static double getPhi(float _value, float _mu, float _sigma) {
        double z = (double) _value;
        double mu = (double) _mu;
        double sigma = (double) _sigma;
        return Phi(z, mu, sigma);
    }

}
