// Required
#include <jni.h>

// We'll likely want these
#include <vector>
#include <string>

// Get any GNU Radio headers
#include <gnuradio/top_block.h>
#include <gnuradio/analog/sig_source_f.h>
#include <gnuradio/blocks/multiply_const_ff.h>
#include <grand/opensl_sink.h>

// Declare the global virtual machine and top-block objects
JavaVM *vm;
gr::top_block_sptr tb;

extern "C" {

JNIEXPORT void JNICALL
Java_org_gnuradio_grtemplatecp_MainActivity_FgInit(JNIEnv* env,
                                                   jobject thiz)
{
  GR_INFO("fg", "FgInit Called");

  float samp_rate = 48e3;  // 48 kHz

  // Declare our GNU Radio blocks
  gr::analog::sig_source_f::sptr src;
  gr::blocks::multiply_const_ff::sptr mult;
  gr::grand::opensl_sink::sptr snk;

  // Construct the objects for every block in the flowgraph
  tb = gr::make_top_block("fg");
  src = gr::analog::sig_source_f::make(samp_rate, gr::analog::GR_SIN_WAVE,
                                       400, 1.0, 0.0);
  mult = gr::blocks::multiply_const_ff::make(0.0);
  snk = gr::grand::opensl_sink::make(int(samp_rate));

  // Connect up the flowgraph
  tb->connect(src, 0, mult, 0);
  tb->connect(mult, 0, snk, 0);
}

JNIEXPORT void JNICALL
Java_org_gnuradio_grtemplatecp_MainActivity_FgStart(JNIEnv* env,
                                                    jobject thiz)
{
  GR_INFO("fg", "FgStart Called");
  tb->start();
}

JNIEXPORT void JNICALL
Java_org_gnuradio_grtemplatecp_MainActivity_FgStop(JNIEnv* env,
                                                   jobject thiz)
{
  GR_INFO("fg", "FgStop Called");
  tb->stop();
  tb->wait();
  GR_INFO("fg", "FgStop Exited");
}

JNIEXPORT jstring JNICALL
Java_org_gnuradio_grtemplatecp_MainActivity_FgRep(JNIEnv* env,
                                                  jobject thiz)
{
  return env->NewStringUTF(tb->edge_list().c_str());
}

}
