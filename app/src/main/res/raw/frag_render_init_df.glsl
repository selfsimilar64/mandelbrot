vec2 colorParams = vec2(0.0);
vec4 c = cadd(cmult(vec2(cosRotate, sinRotate), vec4(mult(viewPos.x, xScale), mult(viewPos.y, yScale))), vec4(xCoord, yCoord));
vec4 z0 = vec4(vec2(x0, 0.0), vec2(y0, 0.0));
vec4 z = z0;
vec4 z1, z2, z3, z4 = vec4(0.0);
vec2 modsqrz = vec2(0.0);
float eps = 0.0;
vec2 sum, sum1 = ZERO;
vec2 modc = cmod(c);
vec2 alpha = alpha0;
vec2 beta = vec2(0.0);
float il = 1.0/log(power);
float llr = log(log(R)/power);
float useUniforms = p1.x + p2.x + p3.x + p4.x + q1.x + q2.x + q3.x + q4.x + x0 + y0 + R;
