#include <stdio.h>
#include <iostream>
#include <iomanip>
#include <time.h>
#include <cstdlib>
#include <omp.h>
#include <chrono>

using namespace std;

#define SYSTEMTIME clock_t

void OnMult(int m_ar, int m_br)
{
   SYSTEMTIME Time1, Time2;

   char st[100];
   double temp;
   int i, j, k;

   double *pha, *phb, *phc;

   pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
   phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
   phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

   for (i = 0; i < m_ar; i++)
      for (j = 0; j < m_ar; j++)
         pha[i * m_ar + j] = 1.0;

   for (i = 0; i < m_br; i++)
      for (j = 0; j < m_br; j++)
         phb[i * m_br + j] = (double)(i + 1);

   Time1 = clock();

   for (i = 0; i < m_ar; i++)
   {
      for (j = 0; j < m_br; j++)
      {
         temp = 0;
         for (k = 0; k < m_ar; k++)
         {
            temp += pha[i * m_ar + k] * phb[k * m_br + j];
         }
         phc[i * m_ar + j] = temp;
      }
   }

   Time2 = clock();
   snprintf(st, sizeof(st), "Time: %3.3f seconds\n",
            (double)(Time2 - Time1) / CLOCKS_PER_SEC);
   cout << st;

   cout << "Result matrix: " << endl;
   for (i = 0; i < 1; i++)
   {
      for (j = 0; j < min(10, m_br); j++)
         cout << phc[j] << " ";
   }
   cout << endl;

   free(pha);
   free(phb);
   free(phc);
}

void OnMultParallel1(int m_ar, int m_br, int n_threads)
{
   typedef chrono::high_resolution_clock clock;
   chrono::time_point<clock> Time1, Time2;

   char st[100];
   double temp;
   int i, j, k;

   double *pha, *phb, *phc;

   pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
   phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
   phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

   for (i = 0; i < m_ar; i++)
      for (j = 0; j < m_ar; j++)
         pha[i * m_ar + j] = 1.0;

   for (i = 0; i < m_br; i++)
      for (j = 0; j < m_br; j++)
         phb[i * m_br + j] = (double)(i + 1);

   Time1 = clock::now();

   #pragma omp parallel for private(i, j, k, temp) num_threads(n_threads)
   for (i = 0; i < m_ar; i++)
   {
      for (j = 0; j < m_br; j++)
      {
         temp = 0;
         for (k = 0; k < m_ar; k++)
         {
            temp += pha[i * m_ar + k] * phb[k * m_br + j];
         }
         phc[i * m_ar + j] = temp;
      }
   }

   Time2 = clock::now();
   snprintf(st, sizeof(st), "Time: %3.3f seconds\n",
            chrono::duration<double>(Time2 - Time1).count());
   cout << st;

   cout << "Result matrix: " << endl;
   for (i = 0; i < 1; i++)
   {
      for (j = 0; j < min(10, m_br); j++)
         cout << phc[j] << " ";
   }
   cout << endl;

   free(pha);
   free(phb);
   free(phc);
}

void OnMultParallel2(int m_ar, int m_br, int n_threads)
{
   typedef chrono::high_resolution_clock clock;
   chrono::time_point<clock> Time1, Time2;

   char st[100];
   double temp;
   int i, j, k;

   double *pha, *phb, *phc;

   pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
   phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
   phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

   for (i = 0; i < m_ar; i++)
      for (j = 0; j < m_ar; j++)
         pha[i * m_ar + j] = 1.0;

   for (i = 0; i < m_br; i++)
      for (j = 0; j < m_br; j++)
         phb[i * m_br + j] = (double)(i + 1);

   for (int x = 0; x < m_ar * m_ar; x++)
      phc[x] = 0.0;

   Time1 = clock::now();

   #pragma omp parallel num_threads(n_threads)
   for (i = 0; i < m_ar; i++)
   {
      for (j = 0; j < m_br; j++)
      {
         temp = 0;
         #pragma omp for
         for (k = 0; k < m_ar; k++)
         {
            temp += pha[i * m_ar + k] * phb[k * m_br + j];
         }
         phc[i * m_ar + j] = temp;
      }
   }

   Time2 = clock::now();
   snprintf(st, sizeof(st), "Time: %3.3f seconds\n",
            chrono::duration<double>(Time2 - Time1).count());
   cout << st;

   cout << "Result matrix: " << endl;
   for (i = 0; i < 1; i++)
   {
      for (j = 0; j < min(10, m_br); j++)
         cout << phc[j] << " ";
   }
   cout << endl;

   free(pha);
   free(phb);
   free(phc);
}

// Line-by-line matrix multiplication
void OnMultLine(int m_ar, int m_br)
{
   SYSTEMTIME Time1, Time2;

   char st[100];
   double temp;
   int i, k, j;

   double *pha, *phb, *phc;

   pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
   phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
   phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

   for (i = 0; i < m_ar; i++)
      for (j = 0; j < m_ar; j++)
         pha[i * m_ar + j] = 1.0;

   for (i = 0; i < m_br; i++)
      for (j = 0; j < m_br; j++)
         phb[i * m_br + j] = (double)(i + 1);

   for (int x = 0; x < m_ar * m_ar; x++)
      phc[x] = 0.0;

   Time1 = clock();

   for (i = 0; i < m_ar; i++)
   {
      for (k = 0; k < m_ar; k++)
      {
         temp = pha[i * m_ar + k];
         for (j = 0; j < m_br; j++)
         {
            phc[i * m_ar + j] += temp * phb[k * m_br + j];
         }
      }
   }

   Time2 = clock();
   snprintf(st, sizeof(st), "Time: %3.3f seconds\n",
            (double)(Time2 - Time1) / CLOCKS_PER_SEC);
   cout << st;

   cout << "Result matrix: " << endl;
   for (i = 0; i < 1; i++)
   {
      for (j = 0; j < min(10, m_br); j++)
         cout << phc[j] << " ";
   }
   cout << endl;

   free(pha);
   free(phb);
   free(phc);
}

// Line-by-line matrix multiplication parallel version 1
void OnMultLineParallel1(int m_ar, int m_br, int n_threads)
{
   typedef chrono::high_resolution_clock clock;
   chrono::time_point<clock> Time1, Time2;

   char st[100];
   int i, k, j;

   double *pha, *phb, *phc;

   pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
   phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
   phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

   for (i = 0; i < m_ar; i++)
      for (j = 0; j < m_ar; j++)
         pha[i * m_ar + j] = 1.0;

   for (i = 0; i < m_br; i++)
      for (j = 0; j < m_br; j++)
         phb[i * m_br + j] = (double)(i + 1);

   for (int x = 0; x < m_ar * m_ar; x++)
      phc[x] = 0.0;

   Time1 = clock::now();

   #pragma omp parallel for private(i, k, j) num_threads(n_threads)
   for (i = 0; i < m_ar; i++)
   {
      for (k = 0; k < m_ar; k++)
      {
         for (j = 0; j < m_br; j++)
         {
            phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
         }
      }
   }

   Time2 = clock::now();
   snprintf(st, sizeof(st), "Time: %3.3f seconds\n",
            chrono::duration<double>(Time2 - Time1).count());
   cout << st;

   cout << "Result matrix: " << endl;
   for (i = 0; i < 1; i++)
   {
      for (j = 0; j < min(10, m_br); j++)
         cout << phc[j] << " ";
   }
   cout << endl;

   free(pha);
   free(phb);
   free(phc);
}

void OnMultLineParallel1Simd(int m_ar, int m_br, int n_threads)
{
   typedef chrono::high_resolution_clock clock;
   chrono::time_point<clock> Time1, Time2;

   char st[100];
   int i, k, j;

   double *pha, *phb, *phc;

   pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
   phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
   phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

   for (i = 0; i < m_ar; i++)
      for (j = 0; j < m_ar; j++)
         pha[i * m_ar + j] = 1.0;

   for (i = 0; i < m_br; i++)
      for (j = 0; j < m_br; j++)
         phb[i * m_br + j] = (double)(i + 1);

   for (int x = 0; x < m_ar * m_ar; x++)
      phc[x] = 0.0;

   Time1 = clock::now();

   #pragma omp parallel for simd private(i, k, j) num_threads(n_threads)
   for (i = 0; i < m_ar; i++)
   {
      for (k = 0; k < m_ar; k++)
      {
         for (j = 0; j < m_br; j++)
         {
            phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
         }
      }
   }

   Time2 = clock::now();
   snprintf(st, sizeof(st), "Time: %3.3f seconds\n",
            chrono::duration<double>(Time2 - Time1).count());
   cout << st;

   cout << "Result matrix: " << endl;
   for (i = 0; i < 1; i++)
   {
      for (j = 0; j < min(10, m_br); j++)
         cout << phc[j] << " ";
   }
   cout << endl;

   free(pha);
   free(phb);
   free(phc);
}

void OnMultLineParallel1Collapse(int m_ar, int m_br, int n_threads)
{
   typedef chrono::high_resolution_clock clock;
   chrono::time_point<clock> Time1, Time2;

   char st[100];
   int i, k, j;

   double *pha, *phb, *phc;

   pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
   phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
   phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

   for (i = 0; i < m_ar; i++)
      for (j = 0; j < m_ar; j++)
         pha[i * m_ar + j] = 1.0;

   for (i = 0; i < m_br; i++)
      for (j = 0; j < m_br; j++)
         phb[i * m_br + j] = (double)(i + 1);

   for (int x = 0; x < m_ar * m_ar; x++)
      phc[x] = 0.0;

   Time1 = clock::now();

   #pragma omp parallel for collapse(2) private(i, j, k) num_threads(n_threads) reduction(+:phc[:m_ar*m_ar])
   for (i = 0; i < m_ar; i++)
   {
      for (k = 0; k < m_ar; k++)
      {
         for (j = 0; j < m_br; j++)
         {
            phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
         }
      }
   }

   Time2 = clock::now();
   snprintf(st, sizeof(st), "Time: %3.3f seconds\n",
            chrono::duration<double>(Time2 - Time1).count());
   cout << st;

   cout << "Result matrix: " << endl;
   for (i = 0; i < 1; i++)
   {
      for (j = 0; j < min(10, m_br); j++)
         cout << phc[j] << " ";
   }
   cout << endl;

   free(pha);
   free(phb);
   free(phc);
}

// Line-by-line matrix multiplication parallel version 2
void OnMultLineParallel2(int m_ar, int m_br, int n_threads)
{
   typedef chrono::high_resolution_clock clock;
   chrono::time_point<clock> Time1, Time2;

   char st[100];
   int i, k, j;

   double *pha, *phb, *phc;

   pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
   phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
   phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

   for (i = 0; i < m_ar; i++)
      for (j = 0; j < m_ar; j++)
         pha[i * m_ar + j] = 1.0;

   for (i = 0; i < m_br; i++)
      for (j = 0; j < m_br; j++)
         phb[i * m_br + j] = (double)(i + 1);

   for (int x = 0; x < m_ar * m_ar; x++)
      phc[x] = 0.0;

   Time1 = clock::now();

   #pragma omp parallel private(i, k, j) num_threads(n_threads)
   for (i = 0; i < m_ar; i++)
   {
      for (k = 0; k < m_ar; k++)
      {
         #pragma omp for
         for (j = 0; j < m_br; j++)
         {
            phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
         }
      }
   }

   Time2 = clock::now();
   snprintf(st, sizeof(st), "Time: %3.3f seconds\n",
            chrono::duration<double>(Time2 - Time1).count());
   cout << st;

   cout << "Result matrix: " << endl;
   for (i = 0; i < 1; i++)
   {
      for (j = 0; j < min(10, m_br); j++)
         cout << phc[j] << " ";
   }
   cout << endl;

   free(pha);
   free(phb);
   free(phc);
}

// Block matrix multiplication
void OnMultBlock(int m_ar, int m_br, int bkSize)
{
   SYSTEMTIME Time1, Time2;

   char st[100];
   double temp;
   int i, j, k, a, b, c;

   double *pha, *phb, *phc;

   pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
   phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
   phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

   for (i = 0; i < m_ar; i++)
      for (j = 0; j < m_ar; j++)
         pha[i * m_ar + j] = 1.0;

   for (i = 0; i < m_br; i++)
      for (j = 0; j < m_br; j++)
         phb[i * m_br + j] = (double)(i + 1);

   for (int x = 0; x < m_ar * m_ar; x++)
      phc[x] = 0.0;

   Time1 = clock();

   // Determine which block we are working on
   for (a = 0; a < m_ar; a += bkSize)
   {
      int i_max = min(a + bkSize, m_ar);
      for (b = 0; b < m_br; b += bkSize)
      {
         int j_max = min(b + bkSize, m_br);
         for (c = 0; c < m_ar; c += bkSize)
         {
            // Create the c block by multiplying A and B
            int k_max = min(c + bkSize, m_ar);
            for (i = a; i < i_max; i++)
            {
               for (k = c; k < k_max; k++)
               {
                  temp = pha[i * m_ar + k];
                  for (j = b; j < j_max; j++)
                  {
                     phc[i * m_ar + j] += temp * phb[k * m_br + j];
                  }
               }
            }
         }
      }
   }

   Time2 = clock();
   snprintf(st, sizeof(st), "Time: %3.3f seconds\n",
            (double)(Time2 - Time1) / CLOCKS_PER_SEC);
   cout << st;

   cout << "Result matrix: " << endl;
   for (i = 0; i < 1; i++)
   {
      for (j = 0; j < min(10, m_br); j++)
         cout << phc[j] << " ";
   }
   cout << endl;

   free(pha);
   free(phb);
   free(phc);
}

int main(int argc, char *argv[])
{
   int lin, col, blockSize;
   int op;

   do
   {
      cout << endl
           << "1. Multiplication" << endl;
      cout << "2. Line Multiplication" << endl;
      cout << "3. Block Multiplication" << endl;
      cout << "0. Exit" << endl;
      cout << "Selection?: ";
      cin >> op;

      if (op == 0)
         break;

      cout << "Dimensions: lins=cols ? ";
      cin >> lin;
      col = lin;

      switch (op)
      {
      case 1:
      {
         int alg;
         cout << "Choose version:\n1. Normal\n2. Parallel 1\n3. Parallel 2\nSelection?: ";
         cin >> alg;
         if (alg == 1)
            OnMult(lin, col);
         else if (alg == 2)
         {
            int n_threads;
            cout << "Number of threads?: ";
            cin >> n_threads;
            OnMultParallel1(lin, col, n_threads);
         }
         else if (alg == 3)
         {
            int n_threads;
            cout << "Number of threads?: ";
            cin >> n_threads;
            OnMultParallel2(lin, col, n_threads);
         }
      }
      break;
      case 2:
      {
         int alg;
         cout << "Choose version:\n1. Normal\n2. Parallel 1\n3. Parallel 2\n4. Parallel 1 SIMD\n5. Parallel 1 Collapse\nSelection?: ";
         cin >> alg;
         if (alg == 1)
            OnMultLine(lin, col);
         else if (alg == 2)
         {
            int n_threads;
            cout << "Number of threads?: ";
            cin >> n_threads;
            OnMultLineParallel1(lin, col, n_threads);
         }
         else if (alg == 3)
         {
            int n_threads;
            cout << "Number of threads?: ";
            cin >> n_threads;
            OnMultLineParallel2(lin, col, n_threads);
         }
         else if (alg == 4)
         {
            int n_threads;
            cout << "Number of threads?: ";
            cin >> n_threads;
            OnMultLineParallel1Simd(lin, col, n_threads);
         }
         else if (alg == 5)
         {
            int n_threads;
            cout << "Number of threads?: ";
            cin >> n_threads;
            OnMultLineParallel1Collapse(lin, col, n_threads);
         }
      }
      break;
      case 3:
         cout << "Block Size? ";
         cin >> blockSize;
         OnMultBlock(lin, col, blockSize);
         break;
      }

   } while (op != 0);

   return 0;
}
