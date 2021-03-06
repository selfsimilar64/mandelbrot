#version 320 es
#define R 1e6
#define pi 3.141592654
#define Sn 1e-30
#define Sp 1e30
#define Sh 1e-15

precision highp float;
uniform int maxIter;
uniform float xTouchPos;
uniform float yTouchPos;
uniform float xScale;
uniform float yScale;
uniform float xOffset;
uniform float yOffset;

in vec4 viewPos;
out vec4 fragmentColor;





int factorial(int n) {
    int prod = 1;
    for (int i = 1; i <= n; i++) {
        prod *= i;
    }
    return prod;
}


float _m(float _a, float _b) {
    return (_a*_b) * Sp;
}


vec2 mandelbrot(vec2 z, vec2 C) {
    return vec2(z.x*z.x - z.y*z.y, 2.0*z.x*z.y) + C;
}

vec2 cExp(vec2 z) {
    float t = exp(z.x);
    return vec2(t*cos(z.y), t*sin(z.y));
}

vec2 cMultSF(vec2 w1, vec2 w2) {
    return vec2(w1.x*w2.x - w1.y*w2.y, w1.x*w2.y + w2.x*w1.y);
}

vec2 _cMultSF(vec2 _a, vec2 _b) {
    return vec2(_m(_a.x, _b.x) - _m(_a.y, _b.y), _m(_a.x, _b.y) + _m(_b.x, _a.y));
}

vec2 _sqr(vec2 _a) {
    return vec2(_m(_a.x, _a.x) - _m(_a.y, _a.y), 2.0*_m(_a.x, _a.y));
}

vec2 conj(vec2 w) {
    return vec2(w.x, -w.y);
}

float modSqrSF(vec2 w) {
    return w.x*w.x + w.y*w.y;
}

float modSF(vec2 w) {
    return sqrt(w.x*w.x + w.y*w.y);
}

float _modSF(vec2 _a) {
    return sqrt(_m(_a.x, _a.x) + _m(_a.y, _a.y)) * Sh;
}

float _modSqrSF(vec2 _a) {
    return _m(_a.x, _a.x) + _m(_a.y, _a.y);
}

vec2 cDivSF(vec2 w1, vec2 w2) {
    vec2 u = cMultSF(w1, conj(w2));
    return u/dot(w2, w2);
}

float atan2(vec2 w) {
    if (w.x > 0.0) {
        return atan(w.y, w.x);
    }
    else if (w.x < 0.0) {
        if (w.y >= 0.0) {
            return atan(w.y, w.x) + pi;
        }
        else {
            return atan(w.y, w.x) - pi;
        }
    }
    else {
        if (w.y > 0.0) {
            return pi/2.0;
        }
        else if (w.y < 0.0) {
            return -pi/2.0;
        }
        else {
            return 0.0;
        }
    }
}

vec2 sine(vec2 z) {
    float u = 0.5*(exp(z.y)+exp(-z.y))*sin(z.x);
    float v = 0.5*(exp(z.y)-exp(-z.y))*cos(z.x);
    return vec2(u, v);
}

vec2 cosine(vec2 z) {
    float u = 0.5*(exp(-z.y) + exp(z.y))*cos(z.x);
    float v = 0.5*(exp(-z.y) - exp(z.y))*sin(z.x);
    return vec2(u, v);
}

vec2 cPow(vec2 z, vec2 s) {
    float theta = atan(z.y, z.x);
    float lr = 0.5*log(dot(z, z));
    float c = exp(s.x*lr - s.y*theta);
    float f = s.y*lr + s.x*theta;
    return c*vec2(cos(f), sin(f));
}

vec2 cPow2(float x, vec2 s) {

    float t;
    if (x == 0.0) { return vec2(0.0); }
    else if (x > 0.0) { t = 0.0; }
    else { t = pi; }

    float c = pow(x, s.x)*exp(-s.y*t);
    float f = s.x*t + s.y*log(x);
    return c*vec2(cos(f), sin(f));

}

vec2 cPow3(vec2 z, float p) {
    float theta = atan(z.y, z.x);
    float r = sqrt(dot(z, z));
    float c = pow(r, p);
    float f = p*theta;
    return c*vec2(cos(f), sin(f));
}

vec2 zeta(vec2 s, int N) {

    vec2 sum, sum1 = vec2(0.0);
    vec2 t;
    int n;
    for (n = 1; n < N; n++) {
        sum1 = sum;
        t = cPow2(float(n), s);
        sum += conj(s) / modSqrSF(s);
    }
    return sum;

}


vec2 arctan_series(vec2 Z, int N) {

    int sign = -1;
    vec2 sum = Z;
    vec2 ZSQR = cMultSF(Z, Z);
    vec2 Zn = Z;

    for (int k = 1; k < N; k++) {
        Zn = cMultSF(Zn, ZSQR);
        float s = float(2*k + 1);
        sum += float(sign) * Zn / s;
        sign *= -1;
    }

    return sum;

}





void main() {

    vec4 colorParams = vec4(0.0);

    float xC = xScale * viewPos.x + xOffset;
    float yC = yScale * viewPos.y + yOffset;


//    vec2 C = vec2(xC, yC);
    vec2 C = vec2(xC, yC);
    vec2 S = vec2(xTouchPos, yTouchPos);
    float CMOD = modSF(C);

//    vec2 C = vec2(1.4686, 1.265);
//    vec2 D = vec2(-0.2013, 0.5638);
    vec2 A = conj(C)/dot(C, C);

    float num_colors = 5.0;
    float cmap_cycles = 5.0;


    float il = 1.0/log(2.0);
    float llr = log(log(R)/2.0);



    float MODZ, Z1MODSQR;
    float sum, sum1 = 0.0;
    float t = 0.0;

    vec2 lightPos = vec2(1.0);
    float height = 1.25;

//    vec2 Z = vec2(1.0, 0.0);
    vec2 Z = C;
    vec2 Z1, Z2 = vec2(0.0);
    vec2 P = vec2(0.);
    float xSqr, ySqr;


    float alpha = 0.5;
    float beta = 2.0;
    float gamma = 0.5;
    float delta = -2.0;


    vec2 _a = vec2(0.0, 0.0);
    vec2 _b = vec2(0.0, 0.0);
    vec2 _u = vec2(0.0, 0.0);
    vec2 v = vec2(0.0, 0.0);

    vec3 tusk    =  vec3(237.0, 205.0, 185.0) / 256.0;
    vec3 black    =  vec3(0.0, 0.0, 0.0);
    vec3 purple   =  vec3(0.3, 0.0, 0.5);
    vec3 red      =  vec3(1.0, 0.0, 0.0);
    vec3 pink     =  vec3(1.0, 0.3, 0.4);
    vec3 yellow   =  vec3(1.0, 1.0, 0.0);
    vec3 white    =  vec3(1.0, 1.0, 1.0);
    vec3 darkblue =  vec3(0.0, 0.15, 0.25);
    vec3 orange   =  vec3(1.0, 0.6, 0.0);

    vec3 yellowish = vec3(1.0, 0.95, 0.75);     // yellow-ish
    vec3 darkblue2 = vec3(0.11, 0.188, 0.35);   // dark blue
    vec3 grass = vec3(0.313, 0.53, 0.45);       // grass



//    vec3 c1 = yellowish;
//    vec3 c2 = darkblue2;
//    vec3 c3 = black;
//    vec3 c4 = 0.9*grass;
//    vec3 c5 = pink;


//    vec3 c1 = vec3(0.0, 0.1, 0.2);
//    vec3 c2 = darkblue;
//    vec3 c3 = vec3(0.7);
//    vec3 c4 = vec3(0.9, 0.4, 0.2);
//    vec3 c5 = purple * 0.5;



    for (int i = 0; i < maxIter; i++) {

        if (i == maxIter - 1) {
            colorParams.w = -1.0;
            break;
        }


        // iterate second derivative
//        _b = 2.0*(cMultSF(_b, Z) + _sqr(_a));

        // iterate derivative
//        _a = 2.0*cMultSF(_a, Z);
//        _a.x = _a.x + Sn;


        // P = 2.0*cMultSF(Z, P) + vec2(1.0, 0.0);
        Z2 = Z1;
        Z1 = Z;

        // Z = mandelbrot(Z, C);
        Z = cMultSF(C, alpha*cPow3(Z, beta) + gamma*cPow3(Z, delta));
        // Z = test(Z, 0.75) + C;
        // Z = zeta(C, 20);
        // Z = cMultSF(Z, cExp(-Z)) + C;
        // Z = sine(cMultSF(Z, C))+C;
        // Z = mandelbrot(Z, C);
        // Z = cDivSF(cMultSF(Z, Z), cMultSF(D, Z) + vec2(1.0, 0.0)) + C;
        // Z = mandelbrot(Z, A);
        // Z = exponential(cDivSF(Z, C)) + C;
        // Z = sine(cMultSF(Z, C));
        // Z = sine(cDivSF(Z, C));
        // Z = sine(cDivSF(Z, cMultSF(C, C)));
        // Z = sine(Z) + C;
        // Z = cosine(cMultSF(Z, Z)) + C;
        // vec2 W = cMultSF(Z, Z);
        // Z = cosine(cDivSF(Z, C));
        // Z = cPow(Z, S) + A;
        // Z = cPow(Z, vec2(-10.0, 10.0)) + C;
        // Z = cMultSF(W, W) + cMultSF(C, C);
        // Z = sine_series(Z, 5) + C;
        // Z = cosine_series(Z, 8) + C;
        // Z = arctan_series(cMultSF(Z, C), 10) + C;


        MODZ = modSF(Z);


//        xSqr = Z.x*Z.x;
//        ySqr = Z.y*Z.y;


//        if (MODZ > R || isinf(xSqr) || isinf(ySqr) || isinf(xSqr + ySqr) || isinf(Z.x*Z.y)) {
        if (MODZ > R) {
//        if (modSqrSF(Z - Z1) < delta) {

            // normal calculation
//            float lo = 0.5*log(MOD2);
//            _u = _cMultSF(  cMultSF(Z, _a), (1.0 + lo)*conj(_sqr(_a)) - lo*conj(cMultSF(Z, _b))  );
//            u = cDivSF(Z, a);
//            v = _u/_modSF(_u);

            // calculate rays for lighting calculations
//            vec3 normRay = vec3(v.x, v.y, 1.0);
//            normRay = normRay / length(normRay);
//            vec3 lightRay = vec3(lightPos.x, lightPos.y, height);
//            lightRay = lightRay / length(lightRay);
//            vec3 viewRay = vec3(0.0, 0.0, 1.0);
//            vec3 reflectRay = 2.0*dot(normRay, lightRay)*normRay - lightRay;

            // calculate lighting components
//            float diffuse = dot(normRay, lightRay);
//            diffuse = diffuse/(1.0 + height);
//            if (diffuse < 0.0) { diffuse = 0.0; }
//            float specular = pow(dot(reflectRay, viewRay), 1.5);
//            if (specular < 0.0) { specular = 0.0; }



            sum /= float(i);
            sum1 /= (float(i - 1));
            float s = il*llr - il*log(0.5*log(modSqrSF(Z1)));
            float r = sum1 + (sum - sum1)*(s + 1.0);
            r /= pi;
//            float n = mod(cmap_cycles*num_colors*r, num_colors);

            colorParams.z = r;


            // normalized values -- finite cycles
//            float m = cmap_cycles*num_colors*float(i)/float(maxIter);
//            float m =  (float(i)-log(0.5*log(MODZ))/log(2.0))/float(maxIter);

//             float n = m - (num_colors * floor(m/num_colors));

            // unnormalized values -- infinite cycles
//            float n = num_colors*float(i)/float(maxIter);
//            float m = float(i)-log(0.5*log(MODZ))/log(2.0);
//            float m = float(i);
//            float n = float(num_colors)/2.0*(cos(m/14.0) + 1.0);
//            float n = float(num_colors)/2.0*(cos(2.0*pow(m + 5.0, 0.4) -  0.3) + 1.0);

//            colorParams.z = n;

//            if      (n >= 0.0 && n < 1.0) {  color = (1.0-n) * c1   +   (n)     * c2;  }
//            else if (n >= 1.0 && n < 2.0) {  color = (2.0-n) * c2   +   (n-1.0) * c3;  }
//            else if (n >= 2.0 && n < 3.0) {  color = (3.0-n) * c3   +   (n-2.0) * c4;  }
//            else if (n >= 3.0 && n < 4.0) {  color = (4.0-n) * c4   +   (n-3.0) * c5;  }
//            else if (n >= 4.0 && n < 5.0) {  color = (5.0-n) * c5   +   (n-4.0) * c1;  }
//            else if (n >= 5.0 && n < 6.0) {  color = (6.0-n) * c6   +   (n-5.0) * c1;  }

//            color = vec3(0.0);
//            color = vec3(1.0 - float(i)/float(maxIter));

//            color = 2.5*(diffuse + 0.2)*color;
//            color = 1.75*(diffuse + 0.2)*color + 0.75*vec3(specular + 0.01);
//            color = vec3(specular);

            // float a = 0.45;
            // float b = 0.8;
            // float c = 0.3;
            // color = vec3((1.0-cos(a*float(i)))/2.0, (1.0-cos(b*float(i)))/2.0, (1.0-cos(c*float(i)))/2.0);


//            color = vec3(1.5*p);
//            color = vec3(t);
//            color = vec3(1.0, 0.0, 1.0);

//            float d = modSF(cDivSF(vec2(2.0*MODZ*log(MODZ), 0.0), P));
//            float w = d/2.0;
//            float f = pow(w/xScale, 1.0/8.0);

//            if (w >= 0.0 && w < 0.05) { color = vec3(r/w); }
//            else { color = vec3(1.0, 0.0, 1.0); }

//            color = vec3(r);
//            color = vec3(atan(C.y, C.x)/(2.0*pi) + 0.5);

            break;

        }


        // == TRIANGLE INEQUALITY ===============================================================
//        sum1 = sum;
//        if (i > 0) {
//            Z1MODSQR = modSqrSF(Z1);
//            float m_n = abs(Z1MODSQR - CMOD);
//            float M_n = Z1MODSQR + CMOD;
//            float p = MODZ - m_n;
//            float q = M_n - m_n;
//            t = p / q;
//            sum += t;
//        }



        // == CURVATURE =========================================================================
//        sum1 = sum;
//        if (i > 1) {
//            vec2 w = cDivSF(Z - Z1, Z1 - Z2);
//            t = abs(atan(w.y, w.x));
//            sum += t;
//        }



        // == STRIPE -- LOOP ====================================================================
        sum1 = sum;
        float ARGZ = atan(Z.y, Z.x);
        sum += 0.5*(sin(5.0*ARGZ) + 1.0);


    }

    fragmentColor = colorParams;
}