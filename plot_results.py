import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

# Read the results
df = pd.read_csv('results.csv')

# Create figure with subplots
fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(15, 12))
fig.suptitle('SIR Simulation Parallel Performance Analysis', fontsize=16)

# 1. Speedup Comparison
ax1.plot(df['Threads'], df['ExecutorSpeedup'], 'b-o', label='ExecutorService', linewidth=2)
ax1.plot(df['Threads'], df['ForkJoinSpeedup'], 'r-s', label='ForkJoinPool', linewidth=2)
ax1.plot(df['Threads'], df['Threads'], 'k--', alpha=0.5, label='Perfect Speedup')
ax1.set_xlabel('Number of Threads')
ax1.set_ylabel('Speedup')
ax1.set_title('Speedup vs Thread Count')
ax1.legend()
ax1.grid(True, alpha=0.3)

# 2. Efficiency Comparison
ax2.plot(df['Threads'], df['ExecutorEfficiency'], 'b-o', label='ExecutorService', linewidth=2)
ax2.plot(df['Threads'], df['ForkJoinEfficiency'], 'r-s', label='ForkJoinPool', linewidth=2)
ax2.axhline(y=100, color='k', linestyle='--', alpha=0.5, label='Perfect Efficiency')
ax2.set_xlabel('Number of Threads')
ax2.set_ylabel('Efficiency (%)')
ax2.set_title('Efficiency vs Thread Count')
ax2.legend()
ax2.grid(True, alpha=0.3)

# 3. Execution Time Comparison
ax3.plot(df['Threads'], df['ExecutorTime'], 'b-o', label='ExecutorService', linewidth=2)
ax3.plot(df['Threads'], df['ForkJoinTime'], 'r-s', label='ForkJoinPool', linewidth=2)
ax3.set_xlabel('Number of Threads')
ax3.set_ylabel('Execution Time (ms)')
ax3.set_title('Execution Time vs Thread Count')
ax3.legend()
ax3.grid(True, alpha=0.3)

# 4. Bar Chart of Best Speedups
best_exec = df.loc[df['ExecutorSpeedup'].idxmax()]
best_fj = df.loc[df['ForkJoinSpeedup'].idxmax()]

methods = ['ExecutorService\n(Best)', 'ForkJoinPool\n(Best)']
speedups = [best_exec['ExecutorSpeedup'], best_fj['ForkJoinSpeedup']]
threads = [best_exec['Threads'], best_fj['Threads']]

bars = ax4.bar(methods, speedups, color=['blue', 'red'], alpha=0.7)
ax4.set_ylabel('Best Speedup')
ax4.set_title('Best Performance Comparison')
ax4.grid(True, alpha=0.3)

# Add value labels on bars
for bar, speedup, thread in zip(bars, speedups, threads):
    height = bar.get_height()
    ax4.text(bar.get_x() + bar.get_width()/2., height,
             f'{speedup:.1f}x\n({int(thread)} threads)',
             ha='center', va='bottom')

plt.tight_layout()
plt.savefig('performance_analysis.png', dpi=300, bbox_inches='tight')
plt.show()

print("\nPerformance Analysis Summary:")
print(f"Best ExecutorService: {best_exec['ExecutorSpeedup']:.2f}x speedup with {int(best_exec['Threads'])} threads")
print(f"Best ForkJoinPool: {best_fj['ForkJoinSpeedup']:.2f}x speedup with {int(best_fj['Threads'])} threads")
print("\nGraph saved as 'performance_analysis.png'")
