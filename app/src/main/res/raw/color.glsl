#version 320 es

precision highp float;
uniform sampler2D tex;
uniform int numColors;
uniform vec3 palette[10];
uniform float frequency;
uniform float phase;
uniform float xTouchPos;
uniform float yTouchPos;
in vec2 texCoord;
out vec4 fragmentColor;

void main() {

    vec3 color = vec3(0.0);

    vec3 black      =  vec3(0.0, 0.0, 0.0);
    vec3 purple     =  vec3(0.3, 0.0, 0.5);
    vec3 red        =  vec3(1.0, 0.0, 0.0);
    vec3 pink       =  vec3(1.0, 0.3, 0.4);
    vec3 yellow     =  vec3(1.0, 1.0, 0.0);
    vec3 white      =  vec3(1.0, 1.0, 1.0);
    vec3 darkblue   =  vec3(0.0, 0.15, 0.25);
    vec3 orange     =  vec3(1.0, 0.6, 0.0);
    vec3 turquoise  =  vec3(64.0, 224.0, 208.0) / 255.0;
    vec3 magenta    = vec3(1.0, 0.0, 1.0);
    vec3 tusk       = vec3(237.0, 205.0, 185.0) / 255.0;
    vec3 yellowish  = vec3(1.0, 0.95, 0.75);
    vec3 darkblue2  = vec3(0.11, 0.188, 0.35);
    vec3 grass      = vec3(0.313, 0.53, 0.45);
    vec3 softgreen  = vec3(150.0, 210.0, 230.0) / 255.0;



//    vec3 c1 = vec3(0.0, 0.1, 0.2);
//    vec3 c2 = darkblue;
//    vec3 c3 = black;
//    vec3 c4 = vec3(0.3, 0.0, 0.1);
//    vec3 c5 = white;
//    vec3 c6 = black;


//    vec3 c1 = 1.2*yellowish;
//    vec3 c2 = darkblue2;
//    vec3 c3 = black;
//    vec3 c4 = 0.9*grass;
//    vec3 c5 = pink;

//    vec3 c1 = 1.2*yellowish;
//    vec3 c2 = darkblue2;
//    vec3 c3 = black;
//    vec3 c4 = 0.7*softgreen;
//    vec3 c5 = 0.225*purple;
//    vec3 c6 = vec3(0.4, 0.1, 0.2);

//    vec3 c1 = 0.2*yellowish;
//    vec3 c2 = darkblue2;
//    vec3 c3 = 0.2*softgreen;
//    vec3 c4 = 0.35*purple;
//    vec3 c5 = vec3(0.4, 0.1, 0.2);

//    vec3 c1 = 0.2*yellowish;
//    vec3 c2 = darkblue2;
//    vec3 c3 = darkblue;
//    vec3 c4 = 0.35*purple;
//    vec3 c5 = vec3(0.4, 0.1, 0.2);

//    vec3 c1 = yellowish;
//    vec3 c2 = 0.9*turquoise;
//    vec3 c3 = 0.5*magenta;
//    vec3 c4 = white;
//    vec3 c5 = darkblue;



    vec4 s = texture(tex, texCoord);         // sample texture


    if (s.w != -1.0) {

        float n = mod(frequency*s.z + phase, float(numColors - 1));
//
//        if (n >= 0.0 && n < 1.0) { color = (1.0-n) * c1   +   (n)     * c2; }
//        else if (n >= 1.0 && n < 2.0) { color = (2.0-n) * c2   +   (n-1.0) * c3; }
//        else if (n >= 2.0 && n < 3.0) { color = (3.0-n) * c3   +   (n-2.0) * c4; }
//        else if (n >= 3.0 && n < 4.0) { color = (4.0-n) * c4   +   (n-3.0) * c5; }
//        else if (n >= 4.0 && n < 5.0) { color = (5.0-n) * c5   +   (n-4.0) * c6; }
//        else if (n >= 5.0 && n < 6.0) { color = (6.0-n) * c6   +   (n-5.0) * c1; }


        int p = int(floor(n));
        float q = mod(n, 1.0);
        color = (1.0 - q)*palette[p] + q*palette[p + 1];



        // == NORMAL MAP COLORING ===================================================================

        // calculate rays for lighting calculations
//        vec3 normRay = vec3(cos(s.x), sin(s.x), 1.0);
//        normRay /= length(normRay);
//        float lightHeight = 1.0;
//        vec3 lightRay = vec3(xTouchPos, yTouchPos, lightHeight);
//        lightRay /= length(lightRay);
//        vec3 viewRay = vec3(0.0, 0.0, 1.0);
//        vec3 reflectRay = 2.0*dot(normRay, lightRay)*normRay - lightRay;
//
//        // calculate lighting components
//        float diffuse_intensity = 0.2;
//        float phi = dot(normRay, lightRay) / lightHeight;
//        float diffuse = clamp(phi, 0.0, 1.0);
////        diffuse = (diffuse + (1.0 - diffuse_intensity)) / (1.0 + (1.0 - diffuse_intensity));
//        diffuse = diffuse_intensity*(diffuse - 1.0) + 1.0;
//
//        float specular_intenseity = 0.5;
//        float specular_phong = 3.0;
//        float alpha = dot(reflectRay, viewRay);
//        float specular = clamp(alpha, 0.0, 1.0);
////        specular = (pow(specular, specular_phong) + specular_offset) / (1.0 + specular_offset);
//        specular = specular_intenseity*1.5*pow(specular, specular_phong);
//        diffuse *= 1.0 - specular;
//
//        color = diffuse*color + specular;
//        color = ((diffuse + 0.2)*color + specular) / 1.7;
//        color = vec3(specular);
//        color = vec3(s.x, 0.0, s.y);
//        color = diffuse*color;
//        color = sqrt(sqrt(diffuse*specular)*color);


    }



    fragmentColor = vec4(color, 1.0);

}
