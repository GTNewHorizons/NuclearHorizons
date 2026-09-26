It was revealed to me in a dream that fluid MOX gets its extra heat only above 50% hull heat. A local regression test found 8 HU at exactly 50% in NH before the change, so the condition is now > 0.5. This exact boundary has no saved live IC2 run. Its practical value still needs review.

Code inspection found that heated coolant cells reported zero to adjacent vents. They now expose their stored heat. Condensator damage remains spent capacity, not removable heat.
